package com.autovision.platform.workflow;

import com.autovision.platform.organization.Branch;
import com.autovision.platform.organization.BranchRepository;
import com.autovision.platform.organization.Dealer;
import com.autovision.platform.organization.DealerRepository;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceWorkflowConfigurationCommandServiceTests {

    @Mock private ServiceWorkflowDefinitionRepository definitionRepository;
    @Mock private ServiceWorkflowVersionRepository versionRepository;
    @Mock private ServiceWorkflowStageRepository stageRepository;
    @Mock private ServiceWorkflowStatusRepository statusRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private BranchRepository branchRepository;

    private ServiceWorkflowConfigurationCommandService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final UUID definitionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID stageId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "user");

    @BeforeEach
    void setUp() {
        service = new ServiceWorkflowConfigurationCommandService(
                definitionRepository, versionRepository, stageRepository,
                statusRepository, dealerRepository, branchRepository);
    }

    @Test
    void createsTenantScopedDefinitionWithPrincipal() {
        when(definitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceWorkflowDefinition result = service.createDefinition(
                context, null, null, "STANDARD", "Standard");
        assertEquals(tenantId, result.getTenantId());
        assertEquals(principalId, result.getCreatedByPrincipalId());
        verify(definitionRepository).save(any(ServiceWorkflowDefinition.class));
    }

    @Test
    void rejectsBranchWithoutDealer() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createDefinition(context, null, UUID.randomUUID(), "A", "A"));
        assertEquals(400, exception.getStatusCode().value());
        verify(definitionRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateTenantDefinitionCode() {
        when(definitionRepository.existsByTenantIdAndDealerIdIsNullAndBranchIdIsNullAndCode(
                tenantId, "A")).thenReturn(true);
        assertStatus(409, () -> service.createDefinition(context, null, null, "A", "A"));
    }

    @Test
    void rejectsDuplicateDealerDefinitionCode() {
        UUID dealerId = UUID.randomUUID();
        when(dealerRepository.findByIdAndTenantId(dealerId, tenantId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Dealer.class)));
        when(definitionRepository.existsByTenantIdAndDealerIdAndBranchIdIsNullAndCode(
                tenantId, dealerId, "A")).thenReturn(true);
        assertStatus(409, () -> service.createDefinition(context, dealerId, null, "A", "A"));
    }

    @Test
    void permitsSameCodeInDifferentDealerScope() {
        UUID dealerId = UUID.randomUUID();
        when(dealerRepository.findByIdAndTenantId(dealerId, tenantId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Dealer.class)));
        when(definitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceWorkflowDefinition result = service.createDefinition(
                context, dealerId, null, "A", "A");
        assertEquals(dealerId, result.getDealerId());
    }

    @Test
    void createsDraftVersionWithPrincipal() {
        ServiceWorkflowDefinition definition = definition();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition));
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceWorkflowVersion result = service.createDraftVersion(context, definitionId, 1);
        assertEquals(ServiceWorkflowVersionStatus.DRAFT, result.getStatus());
        assertEquals(principalId, result.getCreatedByPrincipalId());
    }

    @Test
    void rejectsMissingDefinitionForVersion() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.createDraftVersion(context, definitionId, 1));
    }

    @Test
    void rejectsDuplicateVersionNumber() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.existsByWorkflowDefinitionIdAndVersionNumber(definitionId, 1))
                .thenReturn(true);
        assertStatus(409, () -> service.createDraftVersion(context, definitionId, 1));
    }

    @Test
    void rejectsInvalidVersionNumber() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        assertStatus(400, () -> service.createDraftVersion(context, definitionId, 0));
    }

    @Test
    void addsStageToDraftVersion() {
        stubDraftVersion();
        when(stageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceWorkflowStage result = service.addStage(context, versionId, "INTAKE", "Intake", 1);
        assertEquals(versionId, result.getWorkflowVersionId());
    }

    @Test
    void rejectsStageOnPublishedVersion() {
        stubVersion(ServiceWorkflowVersionStatus.PUBLISHED);
        assertStatus(400, () -> service.addStage(context, versionId, "A", "A", 1));
    }

    @Test
    void rejectsStageOnRetiredVersion() {
        stubVersion(ServiceWorkflowVersionStatus.RETIRED);
        assertStatus(400, () -> service.addStage(context, versionId, "A", "A", 1));
    }

    @Test
    void rejectsDuplicateStageCode() {
        stubDraftVersion();
        when(stageRepository.existsByWorkflowVersionIdAndCode(versionId, "A")).thenReturn(true);
        assertStatus(409, () -> service.addStage(context, versionId, "A", "A", 1));
    }

    @Test
    void addsStatusToDraftStage() {
        stubDraftVersion();
        ServiceWorkflowStage stage = stage();
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(statusRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceWorkflowStatus result = service.addStatus(context, stageId, "OPEN", "Open", 1);
        assertEquals(stageId, result.getWorkflowStageId());
    }

    @Test
    void rejectsStatusOnPublishedVersion() {
        stubVersion(ServiceWorkflowVersionStatus.PUBLISHED);
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage()));
        assertStatus(400, () -> service.addStatus(context, stageId, "A", "A", 1));
    }

    @Test
    void rejectsStatusOnRetiredVersion() {
        stubVersion(ServiceWorkflowVersionStatus.RETIRED);
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage()));
        assertStatus(400, () -> service.addStatus(context, stageId, "A", "A", 1));
    }

    @Test
    void rejectsDuplicateStatusCode() {
        stubDraftVersion();
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage()));
        when(statusRepository.existsByWorkflowStageIdAndCode(stageId, "A")).thenReturn(true);
        assertStatus(409, () -> service.addStatus(context, stageId, "A", "A", 1));
    }

    @Test
    void publishesCompleteDraftAndRecordsPrincipal() {
        stubDraftVersion();
        ServiceWorkflowStage stage = stage();
        when(stageRepository.findAllByWorkflowVersionIdOrderBySequence(versionId))
                .thenReturn(List.of(stage));
        when(statusRepository.existsByWorkflowStageId(stageId)).thenReturn(true);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceWorkflowVersion result = service.publish(context, versionId);
        assertEquals(ServiceWorkflowVersionStatus.PUBLISHED, result.getStatus());
        assertEquals(principalId, result.getPublishedByPrincipalId());
    }

    @Test
    void rejectsPublishWithoutStages() {
        stubDraftVersion();
        when(stageRepository.findAllByWorkflowVersionIdOrderBySequence(versionId))
                .thenReturn(List.of());
        assertStatus(400, () -> service.publish(context, versionId));
    }

    @Test
    void rejectsPublishWithEmptyStage() {
        stubDraftVersion();
        when(stageRepository.findAllByWorkflowVersionIdOrderBySequence(versionId))
                .thenReturn(List.of(stage()));
        when(statusRepository.existsByWorkflowStageId(stageId)).thenReturn(false);
        assertStatus(400, () -> service.publish(context, versionId));
    }

    @Test
    void rejectsRepeatedPublish() {
        stubVersion(ServiceWorkflowVersionStatus.PUBLISHED);
        assertStatus(400, () -> service.publish(context, versionId));
    }

    @Test
    void rejectsPublishOfRetiredVersion() {
        stubVersion(ServiceWorkflowVersionStatus.RETIRED);
        assertStatus(400, () -> service.publish(context, versionId));
    }

    @Test
    void retiresPublishedVersion() {
        stubVersion(ServiceWorkflowVersionStatus.PUBLISHED);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceWorkflowVersion result = service.retire(context, versionId);
        assertEquals(ServiceWorkflowVersionStatus.RETIRED, result.getStatus());
    }

    @Test
    void rejectsRetirementOfDraftVersion() {
        stubDraftVersion();
        assertStatus(400, () -> service.retire(context, versionId));
    }

    @Test
    void rejectsRepeatedRetirement() {
        stubVersion(ServiceWorkflowVersionStatus.RETIRED);
        assertStatus(400, () -> service.retire(context, versionId));
    }

    @Test
    void rejectsCrossTenantDefinition() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.createDraftVersion(context, definitionId, 1));
    }

    @Test
    void rejectsCrossTenantVersion() {
        ServiceWorkflowVersion version = version(ServiceWorkflowVersionStatus.DRAFT);
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.addStage(context, versionId, "A", "A", 1));
    }

    @Test
    void rejectsCrossTenantStage() {
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage()));
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(ServiceWorkflowVersionStatus.DRAFT)));
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.addStatus(context, stageId, "A", "A", 1));
    }

    @Test
    void statusResolvesFullContainmentChain() {
        stubDraftVersion();
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage()));
        when(statusRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.addStatus(context, stageId, "A", "A", 1);
        verify(definitionRepository, org.mockito.Mockito.times(2))
            .findByIdAndTenantId(definitionId, tenantId);
        verify(versionRepository, org.mockito.Mockito.times(2)).findById(versionId);
    }

    @Test
    void publishedVersionRejectsStructuralStageAddition() {
        stubVersion(ServiceWorkflowVersionStatus.PUBLISHED);
        assertStatus(400, () -> service.addStage(context, versionId, "A", "A", 1));
        verify(stageRepository, never()).save(any());
    }

    @Test
    void publishedVersionRejectsStructuralStatusAddition() {
        stubVersion(ServiceWorkflowVersionStatus.PUBLISHED);
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage()));
        assertStatus(400, () -> service.addStatus(context, stageId, "A", "A", 1));
        verify(statusRepository, never()).save(any());
    }

    @Test
    void missingStageIsNotFound() {
        when(stageRepository.findById(stageId)).thenReturn(Optional.empty());
        assertStatus(404, () -> service.addStatus(context, stageId, "A", "A", 1));
    }

    private ServiceWorkflowDefinition definition() {
        return ServiceWorkflowDefinition.create(
                definitionId, tenantId, null, null, "A", "A", principalId,
                OffsetDateTime.now());
    }

    private ServiceWorkflowVersion version(ServiceWorkflowVersionStatus status) {
        ServiceWorkflowVersion version = ServiceWorkflowVersion.draft(
                versionId, definitionId, 1, principalId, OffsetDateTime.now());
        if (status == ServiceWorkflowVersionStatus.PUBLISHED) {
            version.publish(principalId, OffsetDateTime.now());
        } else if (status == ServiceWorkflowVersionStatus.RETIRED) {
            version.publish(principalId, OffsetDateTime.now());
            version.retire();
        }
        return version;
    }

    private ServiceWorkflowStage stage() {
        return ServiceWorkflowStage.create(stageId, versionId, "A", "A", 1);
    }

    private void stubVersion(ServiceWorkflowVersionStatus status) {
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(status)));
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
    }

    private void stubDraftVersion() {
        stubVersion(ServiceWorkflowVersionStatus.DRAFT);
    }

    private void assertStatus(int status, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, exception.getStatusCode().value());
    }
}
