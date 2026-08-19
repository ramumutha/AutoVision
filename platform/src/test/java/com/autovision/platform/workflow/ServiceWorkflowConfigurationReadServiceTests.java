package com.autovision.platform.workflow;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceWorkflowConfigurationReadServiceTests {

    @Mock private ServiceWorkflowDefinitionRepository definitionRepository;
    @Mock private ServiceWorkflowVersionRepository versionRepository;
    @Mock private ServiceWorkflowStageRepository stageRepository;
    @Mock private ServiceWorkflowStatusRepository statusRepository;
    @Mock private ServiceWorkflowTransitionRepository transitionRepository;
    @Mock private AuthorizationService authorizationService;

    private ServiceWorkflowConfigurationReadService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final UUID definitionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID stageId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "user");

    @BeforeEach
    void setUp() {
        service = new ServiceWorkflowConfigurationReadService(
                definitionRepository, versionRepository, stageRepository,
                statusRepository, transitionRepository, authorizationService);
    }

    @Test
    void listDefinitionsRequiresReadAndUsesTenantContainment() {
        when(definitionRepository.findAllByTenantId(tenantId))
                .thenReturn(List.of(definition()));

        assertEquals(1, service.findDefinitions(context).size());

        verify(authorizationService).requirePermission(
                context, ServiceWorkflowPermissions.READ);
        verify(definitionRepository).findAllByTenantId(tenantId);
    }

    @Test
    void deniedListDoesNotAccessRepository() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(authorizationService)
                .requirePermission(context, ServiceWorkflowPermissions.READ);

        assertThrows(AccessDeniedException.class,
                () -> service.findDefinitions(context));

        verifyNoInteractions(definitionRepository);
    }

    @Test
    void requireDefinitionUsesWorkflowResourceAuthorization() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));

        service.requireDefinition(context, definitionId);

        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, ServiceWorkflowPermissions.READ,
                AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
        verify(definitionRepository).findByIdAndTenantId(definitionId, tenantId);
    }

    @Test
    void crossTenantDefinitionIsNotFound() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requireDefinition(context, definitionId));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void findVersionsValidatesDefinitionBeforeListing() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findAllByWorkflowDefinitionIdOrderByVersionNumber(definitionId))
                .thenReturn(List.of(version()));

        assertEquals(1, service.findVersions(context, definitionId).size());
        verify(versionRepository).findAllByWorkflowDefinitionIdOrderByVersionNumber(definitionId);
    }

    @Test
    void requireVersionRejectsVersionBelongingToAnotherDefinition() {
        UUID otherDefinitionId = UUID.randomUUID();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(
                ServiceWorkflowVersion.draft(versionId, otherDefinitionId, 1,
                        principalId, OffsetDateTime.now())));

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.requireVersion(context, definitionId, versionId))
                .getStatusCode().value());
    }

    @Test
    void findStagesRejectsVersionOutsideRequestedDefinition() {
        UUID otherDefinitionId = UUID.randomUUID();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(
                ServiceWorkflowVersion.draft(versionId, otherDefinitionId, 1,
                        principalId, OffsetDateTime.now())));

        assertThrows(ResponseStatusException.class,
                () -> service.findStages(context, definitionId, versionId));

        verifyNoInteractions(stageRepository);
    }

    @Test
    void findStatusesValidatesStageVersionDefinitionChain() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version()));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage()));
        when(statusRepository.findAllByWorkflowStageIdOrderBySequence(stageId))
                .thenReturn(List.of(status()));

        assertEquals(1, service.findStatuses(
                context, definitionId, versionId, stageId).size());
        verify(statusRepository).findAllByWorkflowStageIdOrderBySequence(stageId);
    }

    @Test
    void findStatusesRejectsStageOutsideRequestedVersion() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version()));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(
                ServiceWorkflowStage.create(stageId, UUID.randomUUID(), "A", "A", 1)));

        assertThrows(ResponseStatusException.class,
                () -> service.findStatuses(context, definitionId, versionId, stageId));
        verifyNoInteractions(statusRepository);
    }

    @Test
    void readsDoNotInvokeMutationRepositories() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));

        service.requireDefinition(context, definitionId);

        verify(versionRepository, never()).save(any());
        verify(stageRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    @Test
    void listTransitionsRequiresReadPermissionOnDefinition() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version()));
        when(transitionRepository.findByWorkflowVersionIdOrderBySequenceAsc(versionId))
                .thenReturn(List.of(transition()));

        assertEquals(1, service.findTransitions(context, definitionId, versionId).size());

        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, ServiceWorkflowPermissions.READ,
                AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
    }

    @Test
    void listTransitionsRequiresTenantContainedDefinition() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.findTransitions(context, definitionId, versionId))
                .getStatusCode().value());
        verifyNoInteractions(transitionRepository);
    }

    @Test
    void listTransitionsRejectsVersionBelongingToAnotherDefinition() {
        UUID otherDefinitionId = UUID.randomUUID();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(
                ServiceWorkflowVersion.draft(versionId, otherDefinitionId, 1,
                        principalId, OffsetDateTime.now())));

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.findTransitions(context, definitionId, versionId))
                .getStatusCode().value());
        verifyNoInteractions(transitionRepository);
    }

    @Test
    void listTransitionsReturnsRepositoryOrder() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version()));
        ServiceWorkflowTransition first = transition();
        when(transitionRepository.findByWorkflowVersionIdOrderBySequenceAsc(versionId))
                .thenReturn(List.of(first));

        List<ServiceWorkflowTransition> result =
                service.findTransitions(context, definitionId, versionId);

        assertEquals(List.of(first), result);
        verify(transitionRepository).findByWorkflowVersionIdOrderBySequenceAsc(versionId);
    }

    @Test
    void requireTransitionUsesTransitionAndVersionContainment() {
        UUID transitionId = UUID.randomUUID();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version()));
        when(transitionRepository.findByIdAndWorkflowVersionId(transitionId, versionId))
                .thenReturn(Optional.of(transition()));

        service.requireTransition(context, definitionId, versionId, transitionId);

        verify(transitionRepository).findByIdAndWorkflowVersionId(transitionId, versionId);
    }

    @Test
    void requireTransitionRejectsTransitionBelongingToAnotherVersion() {
        UUID transitionId = UUID.randomUUID();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version()));
        when(transitionRepository.findByIdAndWorkflowVersionId(transitionId, versionId))
                .thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.requireTransition(context, definitionId, versionId, transitionId))
                .getStatusCode().value());
    }

    @Test
    void requireTransitionRejectsCrossTenantDefinition() {
        UUID transitionId = UUID.randomUUID();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.requireTransition(context, definitionId, versionId, transitionId))
                .getStatusCode().value());
        verifyNoInteractions(transitionRepository);
    }

    @Test
    void deniedTransitionListingPreventsDisclosureAfterMinimumLookup() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
                () -> service.findTransitions(context, definitionId, versionId));

        verifyNoInteractions(definitionRepository, versionRepository, transitionRepository);
    }

    @Test
    void transitionReadsDoNotInvokeRuntimeExecutionRepositories() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version()));
        when(transitionRepository.findByWorkflowVersionIdOrderBySequenceAsc(versionId))
                .thenReturn(List.of(transition()));

        service.findTransitions(context, definitionId, versionId);

        verify(transitionRepository, never()).save(any());
        verify(transitionRepository, never()).deleteAll();
    }

    private ServiceWorkflowTransition transition() {
        return ServiceWorkflowTransition.create(
                UUID.randomUUID(), versionId, stageId, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "APPROVE", "Approve", 1,
                principalId, OffsetDateTime.now());
    }

    private ServiceWorkflowDefinition definition() {
        return ServiceWorkflowDefinition.create(
                definitionId, tenantId, null, null, "A", "A", principalId,
                OffsetDateTime.now());
    }

    private ServiceWorkflowVersion version() {
        return ServiceWorkflowVersion.draft(
                versionId, definitionId, 1, principalId, OffsetDateTime.now());
    }

    private ServiceWorkflowStage stage() {
        return ServiceWorkflowStage.create(stageId, versionId, "A", "A", 1);
    }

    private ServiceWorkflowStatus status() {
        return ServiceWorkflowStatus.create(
                UUID.randomUUID(), stageId, "OPEN", "Open", 1);
    }
}