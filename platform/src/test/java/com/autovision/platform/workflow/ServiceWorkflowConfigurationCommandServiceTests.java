package com.autovision.platform.workflow;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.organization.Branch;
import com.autovision.platform.organization.BranchRepository;
import com.autovision.platform.organization.Dealer;
import com.autovision.platform.organization.DealerRepository;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceWorkflowConfigurationCommandServiceTests {

    @Mock private ServiceWorkflowDefinitionRepository definitionRepository;
    @Mock private ServiceWorkflowVersionRepository versionRepository;
    @Mock private ServiceWorkflowStageRepository stageRepository;
    @Mock private ServiceWorkflowStatusRepository statusRepository;
    @Mock private ServiceWorkflowTransitionRepository transitionRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private AuthorizationService authorizationService;

    private ServiceWorkflowConfigurationCommandService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final UUID definitionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID stageId = UUID.randomUUID();
    private final UUID fromStageId = UUID.randomUUID();
    private final UUID fromStatusId = UUID.randomUUID();
    private final UUID toStageId = UUID.randomUUID();
    private final UUID toStatusId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "user");

    @BeforeEach
    void setUp() {
        service = new ServiceWorkflowConfigurationCommandService(
                definitionRepository, versionRepository, stageRepository,
            statusRepository, transitionRepository, dealerRepository, branchRepository,
            authorizationService);
    }

    @Test
    void createsTenantScopedDefinitionWithPrincipal() {
        when(definitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ServiceWorkflowDefinition result = service.createDefinition(
                context, null, null, "STANDARD", "Standard");
        assertEquals(tenantId, result.getTenantId());
        assertEquals(principalId, result.getCreatedByPrincipalId());
        verify(definitionRepository).save(any(ServiceWorkflowDefinition.class));
        verify(authorizationService).requirePermission(new AuthorizationRequest(
            context, ServiceWorkflowPermissions.MANAGE,
            AuthorizationResourceType.TENANT, tenantId));
        }

        @Test
        void createsDealerScopedDefinitionWithDealerAuthorizationTarget() {
        UUID dealerId = UUID.randomUUID();
        when(dealerRepository.findByIdAndTenantId(dealerId, tenantId))
            .thenReturn(Optional.of(org.mockito.Mockito.mock(Dealer.class)));
        when(definitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createDefinition(context, dealerId, null, "A", "A");

        verify(authorizationService).requirePermission(new AuthorizationRequest(
            context, ServiceWorkflowPermissions.MANAGE,
            AuthorizationResourceType.DEALER, dealerId));
        }

        @Test
        void createsBranchScopedDefinitionWithBranchAuthorizationTarget() {
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Branch branch = org.mockito.Mockito.mock(Branch.class);
        when(dealerRepository.findByIdAndTenantId(dealerId, tenantId))
            .thenReturn(Optional.of(org.mockito.Mockito.mock(Dealer.class)));
        when(branchRepository.findByIdAndTenantId(branchId, tenantId))
            .thenReturn(Optional.of(branch));
        when(branch.getDealerId()).thenReturn(dealerId);
        when(definitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createDefinition(context, dealerId, branchId, "A", "A");

        verify(authorizationService).requirePermission(new AuthorizationRequest(
            context, ServiceWorkflowPermissions.MANAGE,
            AuthorizationResourceType.BRANCH, branchId));
        }

        @Test
        void deniedDefinitionCreationOccursBeforeOrganizationAndDuplicateAccess() {
        doThrow(new AccessDeniedException("denied"))
            .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
            () -> service.createDefinition(
                context, UUID.randomUUID(), UUID.randomUUID(), "A", "A"));

        verifyNoInteractions(dealerRepository, branchRepository, definitionRepository);
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
        verify(authorizationService).requirePermission(new AuthorizationRequest(
            context, ServiceWorkflowPermissions.MANAGE,
            AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
        }

        @Test
        void deniedDraftCreationDoesNotSave() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
            .thenReturn(Optional.of(definition()));
        doThrow(new AccessDeniedException("denied"))
            .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
            () -> service.createDraftVersion(context, definitionId, 1));

        verify(versionRepository, never()).save(any());
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
        verify(authorizationService).requirePermission(new AuthorizationRequest(
            context, ServiceWorkflowPermissions.MANAGE,
            AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
        }

        @Test
        void deniedStageAdditionDoesNotSave() {
        stubDraftVersion();
        doThrow(new AccessDeniedException("denied"))
            .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
            () -> service.addStage(context, versionId, "A", "A", 1));

        verify(stageRepository, never()).save(any());
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
        verify(authorizationService).requirePermission(new AuthorizationRequest(
            context, ServiceWorkflowPermissions.MANAGE,
            AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
        }

        @Test
        void deniedStatusAdditionDoesNotSave() {
        stubDraftVersion();
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage()));
        doThrow(new AccessDeniedException("denied"))
            .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
            () -> service.addStatus(context, stageId, "A", "A", 1));

        verify(statusRepository, never()).save(any());
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
        verify(authorizationService).requirePermission(new AuthorizationRequest(
            context, ServiceWorkflowPermissions.PUBLISH,
            AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
        }

        @Test
        void deniedPublishDoesNotSaveOrMutateLifecycle() {
        stubDraftVersion();
        doThrow(new AccessDeniedException("denied"))
            .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
            () -> service.publish(context, versionId));

        verify(versionRepository, never()).save(any());
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
        verify(authorizationService).requirePermission(new AuthorizationRequest(
            context, ServiceWorkflowPermissions.PUBLISH,
            AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
        }

        @Test
        void deniedRetirementDoesNotSaveOrMutateLifecycle() {
        stubVersion(ServiceWorkflowVersionStatus.PUBLISHED);
        doThrow(new AccessDeniedException("denied"))
            .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
            () -> service.retire(context, versionId));

        verify(versionRepository, never()).save(any());
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

    @Test
    void addsSameStageDifferentStatusTransition() {
        stubTransitionPrerequisites();
        stubStage(toStageId, versionId, true, fromStageId);
        stubStatus(toStatusId, fromStageId, true);

        ServiceWorkflowTransition result = service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                fromStageId, toStatusId, "T1", "Transition 1", 1);

        assertEquals(fromStageId, result.getFromStageId());
        assertEquals(fromStageId, result.getToStageId());
        assertEquals(fromStatusId, result.getFromStatusId());
        assertEquals(toStatusId, result.getToStatusId());
    }

    @Test
    void addsValidCrossStageTransition() {
        stubTransitionPrerequisites();

        ServiceWorkflowTransition result = service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "Transition 1", 1);

        assertEquals(versionId, result.getWorkflowVersionId());
        assertEquals(fromStageId, result.getFromStageId());
        assertEquals(fromStatusId, result.getFromStatusId());
        assertEquals(toStageId, result.getToStageId());
        assertEquals(toStatusId, result.getToStatusId());
        assertEquals("T1", result.getCode());
        assertEquals("Transition 1", result.getDisplayName());
        assertEquals(1, result.getSequence());
        assertEquals(principalId, result.getCreatedByPrincipalId());
        verify(transitionRepository).save(any(ServiceWorkflowTransition.class));
    }

    @Test
    void requiresManagePermissionOnDefinitionForTransition() {
        stubTransitionPrerequisites();
        service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "Transition 1", 1);
        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, ServiceWorkflowPermissions.MANAGE,
                AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
    }

    @Test
    void deniedTransitionAuthorizationPreventsFurtherResolutionAndSave() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        doThrow(new AccessDeniedException("denied"))
                .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "Transition 1", 1));

        verifyNoInteractions(versionRepository, stageRepository, statusRepository,
                transitionRepository);
    }

    @Test
    void authorizationOccursBeforeDuplicateChecksAndSave() {
        InOrder inOrder = inOrder(authorizationService, transitionRepository);
        stubTransitionPrerequisites();

        service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "Transition 1", 1);

        inOrder.verify(authorizationService).requirePermission(any(AuthorizationRequest.class));
        inOrder.verify(transitionRepository)
                .existsByWorkflowVersionIdAndCode(versionId, "T1");
        inOrder.verify(transitionRepository).save(any());
    }

    @Test
    void rejectsMissingWorkflowDefinitionForTransition() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsCrossTenantDefinitionForTransition() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
        verifyNoInteractions(versionRepository);
    }

    @Test
    void rejectsVersionBelongingToAnotherDefinition() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        ServiceWorkflowVersion foreignVersion = ServiceWorkflowVersion.draft(
                versionId, UUID.randomUUID(), 1, principalId, OffsetDateTime.now());
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(foreignVersion));

        assertStatus(404, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void acceptsDraftVersionForTransition() {
        stubTransitionPrerequisites();
        assertEquals("T1", service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "Transition 1", 1).getCode());
    }

    @Test
    void rejectsPublishedVersionForTransition() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(ServiceWorkflowVersionStatus.PUBLISHED)));
        assertStatus(400, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsRetiredVersionForTransition() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(ServiceWorkflowVersionStatus.RETIRED)));
        assertStatus(400, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsFromStageBelongingToAnotherVersion() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(ServiceWorkflowVersionStatus.DRAFT)));
        when(stageRepository.findByIdAndWorkflowVersionId(fromStageId, versionId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsFromStatusBelongingToAnotherStage() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(ServiceWorkflowVersionStatus.DRAFT)));
        stubStage(fromStageId, versionId, true);
        when(statusRepository.findByIdAndWorkflowStageId(fromStatusId, fromStageId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsToStageBelongingToAnotherVersion() {
        stubFromResolved();
        when(stageRepository.findByIdAndWorkflowVersionId(toStageId, versionId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsToStatusBelongingToAnotherStage() {
        stubFromResolved();
        stubStage(toStageId, versionId, true);
        when(statusRepository.findByIdAndWorkflowStageId(toStatusId, toStageId))
                .thenReturn(Optional.empty());
        assertStatus(404, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsInactiveFromStage() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(ServiceWorkflowVersionStatus.DRAFT)));
        stubStage(fromStageId, versionId, false);
        assertStatus(400, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsInactiveFromStatus() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(ServiceWorkflowVersionStatus.DRAFT)));
        stubStage(fromStageId, versionId, true);
        stubStatus(fromStatusId, fromStageId, false);
        assertStatus(400, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsInactiveToStage() {
        stubFromResolved();
        stubStage(toStageId, versionId, false);
        assertStatus(400, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsInactiveToStatus() {
        stubFromResolved();
        stubStage(toStageId, versionId, true);
        stubStatus(toStatusId, toStageId, false);
        assertStatus(400, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
    }

    @Test
    void rejectsDuplicateTransitionCode() {
        stubTransitionPrerequisites();
        when(transitionRepository.existsByWorkflowVersionIdAndCode(versionId, "T1"))
                .thenReturn(true);
        assertStatus(409, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
        verify(transitionRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateIdenticalEdge() {
        stubTransitionPrerequisites();
        when(transitionRepository
                .existsByWorkflowVersionIdAndFromStageIdAndFromStatusIdAndToStageIdAndToStatusId(
                        versionId, fromStageId, fromStatusId, toStageId, toStatusId))
                .thenReturn(true);
        assertStatus(409, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "T1", 1));
        verify(transitionRepository, never()).save(any());
    }

    @Test
    void allowsBranchingFromSameOriginToDifferentTargets() {
        stubTransitionPrerequisites();
        UUID otherToStageId = UUID.randomUUID();
        UUID otherToStatusId = UUID.randomUUID();
        stubStage(otherToStageId, versionId, true);
        stubStatus(otherToStatusId, otherToStageId, true);

        ServiceWorkflowTransition result = service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                otherToStageId, otherToStatusId, "T2", "Branch", 2);

        assertEquals(otherToStageId, result.getToStageId());
    }

    @Test
    void allowsConvergenceFromDifferentOriginsToSameTarget() {
        stubTransitionPrerequisites();
        UUID otherFromStageId = UUID.randomUUID();
        UUID otherFromStatusId = UUID.randomUUID();
        stubStage(otherFromStageId, versionId, true);
        stubStatus(otherFromStatusId, otherFromStageId, true);

        ServiceWorkflowTransition result = service.addTransition(
                context, definitionId, versionId, otherFromStageId, otherFromStatusId,
                toStageId, toStatusId, "T3", "Converge", 3);

        assertEquals(toStageId, result.getToStageId());
    }

    @Test
    void rejectsCompleteSelfLoop() {
        stubTransitionPrerequisites();
        stubStage(toStageId, versionId, true, fromStageId);
        stubStatus(toStatusId, fromStageId, true, fromStatusId);

        assertStatus(400, () -> service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                fromStageId, fromStatusId, "T1", "T1", 1));
        verify(transitionRepository, never()).save(any());
    }

    @Test
    void savesTransitionExactlyOnceAndDoesNotMutateParents() {
        stubTransitionPrerequisites();
        service.addTransition(
                context, definitionId, versionId, fromStageId, fromStatusId,
                toStageId, toStatusId, "T1", "Transition 1", 1);

        verify(transitionRepository, org.mockito.Mockito.times(1)).save(any());
        verify(definitionRepository, never()).save(any());
        verify(versionRepository, never()).save(any());
        verify(stageRepository, never()).save(any());
        verify(statusRepository, never()).save(any());
    }

    private void stubFromResolved() {
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition()));
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version(ServiceWorkflowVersionStatus.DRAFT)));
        stubStage(fromStageId, versionId, true);
        stubStatus(fromStatusId, fromStageId, true);
    }

    private void stubTransitionPrerequisites() {
        stubFromResolved();
        stubStage(toStageId, versionId, true);
        stubStatus(toStatusId, toStageId, true);
        lenient().when(transitionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ServiceWorkflowStage stubStage(
            UUID stageId, UUID workflowVersionId, boolean active) {
        return stubStage(stageId, workflowVersionId, active, stageId);
    }

    private ServiceWorkflowStage stubStage(
            UUID stageId, UUID workflowVersionId, boolean active, UUID returnedId) {
        ServiceWorkflowStage stage = mockStage(returnedId, workflowVersionId, active);
        lenient().when(stageRepository.findByIdAndWorkflowVersionId(stageId, workflowVersionId))
                .thenReturn(Optional.of(stage));
        return stage;
    }

    private ServiceWorkflowStatus stubStatus(
            UUID statusId, UUID workflowStageId, boolean active) {
        return stubStatus(statusId, workflowStageId, active, statusId);
    }

    private ServiceWorkflowStatus stubStatus(
            UUID statusId, UUID workflowStageId, boolean active, UUID returnedId) {
        ServiceWorkflowStatus status = mockStatus(returnedId, workflowStageId, active);
        lenient().when(statusRepository.findByIdAndWorkflowStageId(statusId, workflowStageId))
                .thenReturn(Optional.of(status));
        return status;
    }

    private ServiceWorkflowStage mockStage(UUID id, UUID workflowVersionId, boolean active) {
        ServiceWorkflowStage stage = mock(ServiceWorkflowStage.class);
        lenient().when(stage.getId()).thenReturn(id);
        lenient().when(stage.getWorkflowVersionId()).thenReturn(workflowVersionId);
        lenient().when(stage.isActive()).thenReturn(active);
        return stage;
    }

    private ServiceWorkflowStatus mockStatus(UUID id, UUID workflowStageId, boolean active) {
        ServiceWorkflowStatus status = mock(ServiceWorkflowStatus.class);
        lenient().when(status.getId()).thenReturn(id);
        lenient().when(status.getWorkflowStageId()).thenReturn(workflowStageId);
        lenient().when(status.isActive()).thenReturn(active);
        return status;
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
