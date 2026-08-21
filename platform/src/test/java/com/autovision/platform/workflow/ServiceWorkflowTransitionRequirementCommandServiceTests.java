package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ProcessRequirementKey;
import com.autovision.platform.aftersales.ProcessRequirementMode;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceWorkflowTransitionRequirementCommandServiceTests {

    @Mock private ServiceWorkflowDefinitionRepository definitionRepository;
    @Mock private ServiceWorkflowVersionRepository versionRepository;
    @Mock private ServiceWorkflowTransitionRepository transitionRepository;
    @Mock private ServiceWorkflowTransitionRequirementRepository requirementRepository;
    @Mock private AuthorizationService authorizationService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final UUID definitionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID transitionId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "user");
    private ServiceWorkflowTransitionRequirementCommandService service;

    @BeforeEach
    void setUp() {
        service = new ServiceWorkflowTransitionRequirementCommandService(
                definitionRepository,
                versionRepository,
                transitionRepository,
                requirementRepository,
                authorizationService
        );
    }

    @Test
    void addsRequirementToTenantContainedDraftTransition() {
        stubDraft();
        when(requirementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceWorkflowTransitionRequirement result = service.addTransitionRequirement(
                context,
                versionId,
                transitionId,
                new ProcessRequirementKey("SERVICE.CUSTOMER_AUTHORIZATION"),
                ProcessRequirementMode.OPTIONAL
        );

        assertEquals(versionId, result.getWorkflowVersionId());
        assertEquals(transitionId, result.getWorkflowTransitionId());
        assertEquals(ProcessRequirementMode.OPTIONAL, result.getRequirementMode());
        verify(requirementRepository).save(any(ServiceWorkflowTransitionRequirement.class));
    }

    @Test
    void rejectsTransitionFromAnotherVersion() {
        when(versionRepository.findById(versionId)).thenReturn(
                Optional.of(version(ServiceWorkflowVersionStatus.DRAFT))
        );
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(transitionRepository.findByIdAndWorkflowVersionId(transitionId, versionId))
                .thenReturn(Optional.empty());

        assertStatus(404, () -> service.addTransitionRequirement(
                context, versionId, transitionId,
                new ProcessRequirementKey("SERVICE.QUOTE"),
                ProcessRequirementMode.REQUIRED
        ));
        verify(transitionRepository).findByIdAndWorkflowVersionId(
                transitionId,
                versionId
        );
        verify(requirementRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownVersionAndPublishedVersion() {
        when(versionRepository.findById(versionId)).thenReturn(Optional.empty());
        assertStatus(404, () -> service.addTransitionRequirement(
                context, versionId, transitionId,
                new ProcessRequirementKey("SERVICE.QUOTE"),
                ProcessRequirementMode.REQUIRED
        ));

        ServiceWorkflowVersion published = version(ServiceWorkflowVersionStatus.PUBLISHED);
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(published));
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        assertStatus(400, () -> service.addTransitionRequirement(
                context, versionId, transitionId,
                new ProcessRequirementKey("SERVICE.QUOTE"),
                ProcessRequirementMode.REQUIRED
        ));
    }

    @Test
    void rejectsDuplicateRequirementKeyOnTransition() {
        stubDraft();
        when(requirementRepository.existsByWorkflowTransitionIdAndRequirementKey(
                transitionId,
                "SERVICE.QUOTE"
        )).thenReturn(true);

        assertStatus(409, () -> service.addTransitionRequirement(
                context, versionId, transitionId,
                new ProcessRequirementKey("SERVICE.QUOTE"),
                ProcessRequirementMode.REQUIRED
        ));
        verify(requirementRepository, never()).save(any());
    }

    private void stubDraft() {
        ServiceWorkflowTransition transition =
                org.mockito.Mockito.mock(ServiceWorkflowTransition.class);
        when(transition.getId()).thenReturn(transitionId);
        when(versionRepository.findById(versionId)).thenReturn(
                Optional.of(version(ServiceWorkflowVersionStatus.DRAFT))
        );
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(transitionRepository.findByIdAndWorkflowVersionId(transitionId, versionId))
                .thenReturn(Optional.of(transition));
    }

    private ServiceWorkflowDefinition definition() {
        return ServiceWorkflowDefinition.create(
                definitionId,
                tenantId,
                null,
                null,
                "STANDARD",
                "Standard",
                principalId,
                OffsetDateTime.now()
        );
    }

    private ServiceWorkflowVersion version(ServiceWorkflowVersionStatus status) {
        ServiceWorkflowVersion version = ServiceWorkflowVersion.draft(
                versionId,
                definitionId,
                1,
                principalId,
                OffsetDateTime.now()
        );
        if (status == ServiceWorkflowVersionStatus.PUBLISHED) {
            version.publish(principalId, OffsetDateTime.now());
        }
        return version;
    }

    private void assertStatus(int status, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, exception.getStatusCode().value());
    }
}