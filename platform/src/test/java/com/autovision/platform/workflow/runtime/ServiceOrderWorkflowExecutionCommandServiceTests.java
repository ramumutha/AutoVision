package com.autovision.platform.workflow.runtime;

import com.autovision.platform.aftersales.AfterSalesPermissions;
import com.autovision.platform.aftersales.ServiceOrder;
import com.autovision.platform.aftersales.ServiceOrderRepository;
import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.workflow.ServiceWorkflowDefinition;
import com.autovision.platform.workflow.ServiceWorkflowDefinitionRepository;
import com.autovision.platform.workflow.ServiceWorkflowPermissions;
import com.autovision.platform.workflow.ServiceWorkflowStage;
import com.autovision.platform.workflow.ServiceWorkflowStageRepository;
import com.autovision.platform.workflow.ServiceWorkflowStatus;
import com.autovision.platform.workflow.ServiceWorkflowStatusRepository;
import com.autovision.platform.workflow.ServiceWorkflowTransition;
import com.autovision.platform.workflow.ServiceWorkflowTransitionRepository;
import com.autovision.platform.workflow.ServiceWorkflowVersion;
import com.autovision.platform.workflow.ServiceWorkflowVersionRepository;
import com.autovision.platform.workflow.ServiceWorkflowVersionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderWorkflowExecutionCommandServiceTests {

    @Mock private ServiceOrderRepository orderRepository;
    @Mock private ServiceOrderWorkflowExecutionRepository executionRepository;
    @Mock private ServiceWorkflowDefinitionRepository definitionRepository;
    @Mock private ServiceWorkflowVersionRepository versionRepository;
    @Mock private ServiceWorkflowStageRepository stageRepository;
    @Mock private ServiceWorkflowStatusRepository statusRepository;
    @Mock private ServiceWorkflowTransitionRepository transitionRepository;
    @Mock private ServiceOrderWorkflowTransitionHistoryRepository historyRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private ServiceOrder order;
    @Mock private ServiceWorkflowDefinition definition;
    @Mock private ServiceWorkflowVersion version;
    @Mock private ServiceWorkflowStage stage;
    @Mock private ServiceWorkflowStatus workflowStatus;

    private ServiceOrderWorkflowExecutionCommandService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID definitionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID stageId = UUID.randomUUID();
    private final UUID statusId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "user");

    @BeforeEach
    void setUp() {
        service = new ServiceOrderWorkflowExecutionCommandService(
                orderRepository, executionRepository, definitionRepository,
                versionRepository, stageRepository, statusRepository,
                transitionRepository, historyRepository, authorizationService);
    }

    @Test
    void assignsValidPublishedWorkflowAndPinsAllIdentities() {
        stubValidAssignment();
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderWorkflowExecution result = service.assign(
                context, orderId, definitionId, versionId, stageId, statusId);

        assertEquals(tenantId, result.getTenantId());
        assertEquals(orderId, result.getServiceOrderId());
        assertEquals(definitionId, result.getWorkflowDefinitionId());
        assertEquals(versionId, result.getWorkflowVersionId());
        assertEquals(stageId, result.getCurrentStageId());
        assertEquals(statusId, result.getCurrentStatusId());
        assertEquals(principalId, result.getCreatedByPrincipalId());
        assertEquals(principalId, result.getUpdatedByPrincipalId());
        verify(executionRepository).save(any(ServiceOrderWorkflowExecution.class));
    }

    @Test
    void requiresServiceOrderUpdatePermissionOnOrderResource() {
        stubValidAssignment();
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.assign(context, orderId, definitionId, versionId, stageId, statusId);

        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                AuthorizationResourceType.SERVICE_ORDER, orderId));
    }

    @Test
    void requiresWorkflowReadPermissionOnDefinitionResource() {
        stubValidAssignment();
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.assign(context, orderId, definitionId, versionId, stageId, statusId);

        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, ServiceWorkflowPermissions.READ,
                AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
    }

    @Test
    void serviceOrderAuthorizationDenialStopsSubsequentWork() {
        doThrow(new AccessDeniedException("denied"))
                .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class, () -> service.assign(
                context, orderId, definitionId, versionId, stageId, statusId));

        verifyNoInteractions(orderRepository, executionRepository,
                definitionRepository, versionRepository, stageRepository,
                statusRepository);
    }

    @Test
    void workflowAuthorizationDenialPreventsSave() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.existsByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(false);
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition));
                org.mockito.Mockito.doAnswer(invocation -> {
                        AuthorizationRequest request = invocation.getArgument(0);
                        if (ServiceWorkflowPermissions.READ.equals(request.permissionCode())) {
                                throw new AccessDeniedException("denied");
                        }
                        return null;
                }).when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class, () -> service.assign(
                context, orderId, definitionId, versionId, stageId, statusId));

        verify(executionRepository, never()).save(any());
        verifyNoInteractions(versionRepository, stageRepository, statusRepository);
    }

    @Test
    void authorizationPrecedesOrderLookupAndWorkflowAuthorizationPrecedesVersionLookup() {
        stubValidAssignment();
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        InOrder order = org.mockito.Mockito.inOrder(authorizationService,
                orderRepository, executionRepository, definitionRepository,
                versionRepository, stageRepository, statusRepository);

        service.assign(context, orderId, definitionId, versionId, stageId, statusId);

        order.verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                AuthorizationResourceType.SERVICE_ORDER, orderId));
        order.verify(orderRepository).findByIdAndTenantId(orderId, tenantId);
        order.verify(executionRepository)
                .existsByServiceOrderIdAndTenantId(orderId, tenantId);
        order.verify(definitionRepository).findByIdAndTenantId(definitionId, tenantId);
        order.verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, ServiceWorkflowPermissions.READ,
                AuthorizationResourceType.SERVICE_WORKFLOW, definitionId));
        order.verify(versionRepository).findById(versionId);
        order.verify(executionRepository).save(any(ServiceOrderWorkflowExecution.class));
    }

    @Test
    void crossTenantOrMissingOrderIsRejectedBeforeConfiguration() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.assign(
                context, orderId, definitionId, versionId, stageId, statusId))
                .getStatusCode().value());
        verifyNoInteractions(definitionRepository, versionRepository,
                stageRepository, statusRepository);
    }

    @Test
    void duplicateAssignmentIsConflictBeforeDefinitionResolution() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.existsByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(true);

        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.assign(
                context, orderId, definitionId, versionId, stageId, statusId))
                .getStatusCode().value());
        verifyNoInteractions(definitionRepository, versionRepository,
                stageRepository, statusRepository);
    }

    @Test
    void missingDefinitionIsNotFound() {
        stubOrderAndNoDuplicate();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.assign(
                context, orderId, definitionId, versionId, stageId, statusId))
                .getStatusCode().value());
    }

    @Test
    void versionMustBelongToDefinition() {
        stubOrderAndDefinition();
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(version.getWorkflowDefinitionId()).thenReturn(UUID.randomUUID());

        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.assign(
                context, orderId, definitionId, versionId, stageId, statusId))
                .getStatusCode().value());
    }

    @Test
    void draftAndRetiredVersionsAreRejected() {
        stubOrderAndDefinition();
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(version.getWorkflowDefinitionId()).thenReturn(definitionId);

        when(version.getStatus()).thenReturn(ServiceWorkflowVersionStatus.DRAFT);
        assertEquals(400, statusOfAssignment());
        when(version.getStatus()).thenReturn(ServiceWorkflowVersionStatus.RETIRED);
        assertEquals(400, statusOfAssignment());
        verifyNoInteractions(stageRepository, statusRepository);
    }

    @Test
    void missingStageOrStageFromAnotherVersionIsRejected() {
        stubPublishedVersion();
        when(stageRepository.findByIdAndWorkflowVersionId(stageId, versionId))
                .thenReturn(Optional.empty());

        assertEquals(404, statusOfAssignment());
        verifyNoInteractions(statusRepository);
    }

    @Test
    void inactiveStageIsRejected() {
        stubPublishedVersion();
        when(stageRepository.findByIdAndWorkflowVersionId(stageId, versionId))
                .thenReturn(Optional.of(stage));
        when(stage.isActive()).thenReturn(false);

        assertEquals(400, statusOfAssignment());
        verifyNoInteractions(statusRepository);
    }

    @Test
    void missingStatusOrStatusFromAnotherStageIsRejected() {
        stubPublishedVersion();
        when(stageRepository.findByIdAndWorkflowVersionId(stageId, versionId))
                .thenReturn(Optional.of(stage));
        when(stage.isActive()).thenReturn(true);
        when(statusRepository.findByIdAndWorkflowStageId(statusId, stageId))
                .thenReturn(Optional.empty());

        assertEquals(404, statusOfAssignment());
    }

    @Test
    void inactiveStatusIsRejected() {
        stubPublishedVersion();
        when(stageRepository.findByIdAndWorkflowVersionId(stageId, versionId))
                .thenReturn(Optional.of(stage));
        when(stage.isActive()).thenReturn(true);
        when(statusRepository.findByIdAndWorkflowStageId(statusId, stageId))
                .thenReturn(Optional.of(workflowStatus));
        when(workflowStatus.isActive()).thenReturn(false);

        assertEquals(400, statusOfAssignment());
        verify(executionRepository, never()).save(any());
    }

    @Test
    void orderAndConfigurationAreNotMutated() {
        stubValidAssignment();
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.assign(context, orderId, definitionId, versionId, stageId, statusId);

        verifyNoInteractions(order, definition);
        verify(version).getWorkflowDefinitionId();
        verify(version).getStatus();
        verify(stage).isActive();
        verify(workflowStatus).isActive();
    }

    @Test
    void missingIdsAreBadRequest() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.assign(
                context, null, definitionId, versionId, stageId, statusId))
                .getStatusCode().value());
        verifyNoInteractions(authorizationService);
    }

    @Test
    void executesValidSameStageDifferentStatusTransition() {
        UUID newStatusId = UUID.randomUUID();
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(stageId, newStatusId);
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderWorkflowExecution result = service.transition(
                context, orderId, transition.getId());

        assertEquals(stageId, result.getCurrentStageId());
        assertEquals(newStatusId, result.getCurrentStatusId());
    }

    @Test
    void executesValidCrossStageTransition() {
        UUID newStageId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(newStageId, newStatusId);
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderWorkflowExecution result = service.transition(
                context, orderId, transition.getId());

        assertEquals(newStageId, result.getCurrentStageId());
        assertEquals(newStatusId, result.getCurrentStatusId());
    }

    @Test
    void transitionRequiresServiceOrderUpdatePermission() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(stageId, UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.transition(context, orderId, transition.getId());

        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                AuthorizationResourceType.SERVICE_ORDER, orderId));
    }

    @Test
    void transitionAuthorizationDenialPreventsSubsequentAccess() {
        doThrow(new AccessDeniedException("denied"))
                .when(authorizationService).requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
                () -> service.transition(context, orderId, UUID.randomUUID()));

        verifyNoInteractions(orderRepository, executionRepository, transitionRepository,
                stageRepository, statusRepository, historyRepository);
    }

    @Test
    void transitionMissingContainedServiceOrderIsNotFound() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.empty());

        assertEquals(404, statusOfTransition(UUID.randomUUID()));
        verifyNoInteractions(executionRepository, transitionRepository, historyRepository);
    }

    @Test
    void transitionMissingRuntimeExecutionIsNotFound() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.empty());

        assertEquals(404, statusOfTransition(UUID.randomUUID()));
        verifyNoInteractions(transitionRepository, historyRepository);
    }

    @Test
    void transitionMustBelongToExecutionWorkflowVersion() {
        ServiceOrderWorkflowExecution execution = execution();
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution));
        UUID transitionId = UUID.randomUUID();
        when(transitionRepository.findByIdAndWorkflowVersionId(transitionId, versionId))
                .thenReturn(Optional.empty());

        assertEquals(404, statusOfTransition(transitionId));
        verifyNoInteractions(stageRepository, statusRepository, historyRepository);
        verify(executionRepository, never()).save(any());
    }

    @Test
    void inactiveTransitionIsRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = org.mockito.Mockito.mock(
                ServiceWorkflowTransition.class);
        UUID transitionId = UUID.randomUUID();
        when(transition.isActive()).thenReturn(false);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution));
        when(transitionRepository.findByIdAndWorkflowVersionId(transitionId, versionId))
                .thenReturn(Optional.of(transition));

        assertEquals(400, statusOfTransition(transitionId));
        verify(executionRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void fromStageMismatchIsRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transitionFrom(
                UUID.randomUUID(), statusId, UUID.randomUUID(), UUID.randomUUID());
        stubExecutionAndTransition(execution, transition);

        assertEquals(409, statusOfTransition(transition.getId()));
        verify(executionRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void fromStatusMismatchIsRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transitionFrom(
                stageId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        stubExecutionAndTransition(execution, transition);

        assertEquals(409, statusOfTransition(transition.getId()));
        verify(executionRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void fromStageAndStatusMismatchIsRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transitionFrom(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        stubExecutionAndTransition(execution, transition);

        assertEquals(409, statusOfTransition(transition.getId()));
        verify(executionRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void exactFromStateIsAccepted() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.transition(context, orderId, transition.getId());

        verify(executionRepository).save(any());
    }

    @Test
    void toStageLoadedUnderPinnedWorkflowVersion() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.transition(context, orderId, transition.getId());

        verify(stageRepository).findByIdAndWorkflowVersionId(
                transition.getToStageId(), versionId);
    }

    @Test
    void toStageBelongingToAnotherVersionIsRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution));
        when(transitionRepository.findByIdAndWorkflowVersionId(transition.getId(), versionId))
                .thenReturn(Optional.of(transition));
        when(stageRepository.findByIdAndWorkflowVersionId(
                transition.getToStageId(), versionId)).thenReturn(Optional.empty());

        assertEquals(404, statusOfTransition(transition.getId()));
        verifyNoInteractions(statusRepository, historyRepository);
        verify(executionRepository, never()).save(any());
    }

    @Test
    void inactiveToStageIsRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution));
        when(transitionRepository.findByIdAndWorkflowVersionId(transition.getId(), versionId))
                .thenReturn(Optional.of(transition));
        when(stageRepository.findByIdAndWorkflowVersionId(
                transition.getToStageId(), versionId)).thenReturn(Optional.of(stage));
        when(stage.isActive()).thenReturn(false);

        assertEquals(400, statusOfTransition(transition.getId()));
        verifyNoInteractions(statusRepository, historyRepository);
        verify(executionRepository, never()).save(any());
    }

    @Test
    void toStatusLoadedUnderToStage() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.transition(context, orderId, transition.getId());

        verify(statusRepository).findByIdAndWorkflowStageId(
                transition.getToStatusId(), transition.getToStageId());
    }

    @Test
    void toStatusBelongingToAnotherStageIsRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution));
        when(transitionRepository.findByIdAndWorkflowVersionId(transition.getId(), versionId))
                .thenReturn(Optional.of(transition));
        when(stageRepository.findByIdAndWorkflowVersionId(
                transition.getToStageId(), versionId)).thenReturn(Optional.of(stage));
        when(stage.isActive()).thenReturn(true);
        when(stage.getId()).thenReturn(transition.getToStageId());
        when(statusRepository.findByIdAndWorkflowStageId(
                transition.getToStatusId(), transition.getToStageId()))
                .thenReturn(Optional.empty());

        assertEquals(404, statusOfTransition(transition.getId()));
        verify(executionRepository, never()).save(any());
    }

    @Test
    void inactiveToStatusIsRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution));
        when(transitionRepository.findByIdAndWorkflowVersionId(transition.getId(), versionId))
                .thenReturn(Optional.of(transition));
        when(stageRepository.findByIdAndWorkflowVersionId(
                transition.getToStageId(), versionId)).thenReturn(Optional.of(stage));
        when(stage.isActive()).thenReturn(true);
        when(stage.getId()).thenReturn(transition.getToStageId());
        when(statusRepository.findByIdAndWorkflowStageId(
                transition.getToStatusId(), transition.getToStageId()))
                .thenReturn(Optional.of(workflowStatus));
        when(workflowStatus.isActive()).thenReturn(false);

        assertEquals(400, statusOfTransition(transition.getId()));
        verify(executionRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void moveToCalledWithExactTransitionTargetIdentities() {
        UUID newStageId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(newStageId, newStatusId);
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderWorkflowExecution result = service.transition(
                context, orderId, transition.getId());

        assertEquals(newStageId, result.getCurrentStageId());
        assertEquals(newStatusId, result.getCurrentStatusId());
    }

    @Test
    void transitionPropagatesAuthenticatedPrincipal() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderWorkflowExecution result = service.transition(
                context, orderId, transition.getId());

        assertEquals(principalId, result.getUpdatedByPrincipalId());
    }

    @Test
    void transitionUpdatedAtAdvances() {
        ServiceOrderWorkflowExecution execution = execution();
        OffsetDateTime createdAt = execution.getUpdatedAt();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderWorkflowExecution result = service.transition(
                context, orderId, transition.getId());

        assertEquals(true, result.getUpdatedAt().isAfter(createdAt)
                || result.getUpdatedAt().isEqual(createdAt));
    }

    @Test
    void transitionSavesExecutionExactlyOnce() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.transition(context, orderId, transition.getId());

        verify(executionRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void transitionNotSavedOnValidationFailure() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transitionFrom(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        stubExecutionAndTransition(execution, transition);

        assertThrows(ResponseStatusException.class,
                () -> service.transition(context, orderId, transition.getId()));

        verify(executionRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void transitionDoesNotMutateServiceOrder() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.transition(context, orderId, transition.getId());

        verifyNoInteractions(order);
    }

    @Test
    void retiredPinnedVersionIsNotAutomaticallyRejected() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        verifyNoInteractions(versionRepository, definitionRepository);

        ServiceOrderWorkflowExecution result = service.transition(
                context, orderId, transition.getId());

        assertEquals(transition.getToStageId(), result.getCurrentStageId());
    }

    @Test
    void transitionDoesNotAcceptTargetStageOrStatusFromCaller() throws NoSuchMethodException {
        java.lang.reflect.Method method = ServiceOrderWorkflowExecutionCommandService.class
                .getMethod("transition", AuthenticatedTenantContext.class, UUID.class, UUID.class);
        assertEquals(3, method.getParameterCount());
    }

    @Test
    void successfulSameStageTransitionWritesOneHistoryRow() {
        UUID newStatusId = UUID.randomUUID();
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(stageId, newStatusId);
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.transition(context, orderId, transition.getId());

        verify(historyRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void successfulCrossStageTransitionWritesOneHistoryRow() {
        UUID newStageId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(newStageId, newStatusId);
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.transition(context, orderId, transition.getId());

        verify(historyRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void historyRecordCapturesExactIdentitiesFromStateAndToState() {
        UUID newStageId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(newStageId, newStatusId);
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<ServiceOrderWorkflowTransitionHistory> captor =
                ArgumentCaptor.forClass(ServiceOrderWorkflowTransitionHistory.class);

        service.transition(context, orderId, transition.getId());

        verify(historyRepository).save(captor.capture());
        ServiceOrderWorkflowTransitionHistory history = captor.getValue();
        assertEquals(tenantId, history.getTenantId());
        assertEquals(orderId, history.getServiceOrderId());
        assertEquals(execution.getId(), history.getWorkflowExecutionId());
        assertEquals(definitionId, history.getWorkflowDefinitionId());
        assertEquals(versionId, history.getWorkflowVersionId());
        assertEquals(transition.getId(), history.getTransitionId());
        assertEquals(stageId, history.getFromStageId());
        assertEquals(statusId, history.getFromStatusId());
        assertEquals(newStageId, history.getToStageId());
        assertEquals(newStatusId, history.getToStatusId());
        assertEquals(principalId, history.getExecutedByPrincipalId());
    }

    @Test
    void executionUpdatedAtAndHistoryExecutedAtShareCommandTimestamp() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<ServiceOrderWorkflowTransitionHistory> captor =
                ArgumentCaptor.forClass(ServiceOrderWorkflowTransitionHistory.class);

        ServiceOrderWorkflowExecution result = service.transition(
                context, orderId, transition.getId());

        verify(historyRepository).save(captor.capture());
        assertEquals(result.getUpdatedAt(), captor.getValue().getExecutedAt());
    }

    @Test
    void executionSaveOccursBeforeHistorySave() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        InOrder inOrder = org.mockito.Mockito.inOrder(executionRepository, historyRepository);

        service.transition(context, orderId, transition.getId());

        inOrder.verify(executionRepository).save(any());
        inOrder.verify(historyRepository).save(any());
    }

    @Test
    void assignmentDoesNotWriteHistory() {
        stubValidAssignment();
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.assign(context, orderId, definitionId, versionId, stageId, statusId);

        verifyNoInteractions(historyRepository);
    }

    @Test
    void historyPersistenceExceptionIsNotSwallowed() {
        ServiceOrderWorkflowExecution execution = execution();
        ServiceWorkflowTransition transition = transition(UUID.randomUUID(), UUID.randomUUID());
        stubValidTransition(execution, transition);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("history persistence failed"))
                .when(historyRepository).save(any());

        assertThrows(RuntimeException.class,
                () -> service.transition(context, orderId, transition.getId()));
    }

    @Test
    void transitionMethodRemainsTransactional() throws NoSuchMethodException {
        java.lang.reflect.Method method = ServiceOrderWorkflowExecutionCommandService.class
                .getMethod("transition", AuthenticatedTenantContext.class, UUID.class, UUID.class);
        assertEquals(true, method.isAnnotationPresent(
                org.springframework.transaction.annotation.Transactional.class));
    }

    @Test
    void transitionHasNoServiceOrderStatusDependency() throws NoSuchMethodException {
        java.lang.reflect.Method method = ServiceOrderWorkflowExecutionCommandService.class
                .getMethod("transition", AuthenticatedTenantContext.class, UUID.class, UUID.class);
        for (Class<?> parameterType : method.getParameterTypes()) {
            org.junit.jupiter.api.Assertions.assertNotEquals(
                    com.autovision.platform.aftersales.ServiceOrderStatus.class, parameterType);
        }
    }

    private int statusOfTransition(UUID transitionId) {
        return assertThrows(ResponseStatusException.class, () -> service.transition(
                context, orderId, transitionId))
                .getStatusCode().value();
    }

    private ServiceOrderWorkflowExecution execution() {
        return ServiceOrderWorkflowExecution.start(
                UUID.randomUUID(), tenantId, orderId, definitionId, versionId,
                stageId, statusId, principalId, OffsetDateTime.now());
    }

    private ServiceWorkflowTransition transition(UUID toStageIdValue, UUID toStatusIdValue) {
        return transitionFrom(stageId, statusId, toStageIdValue, toStatusIdValue);
    }

    private ServiceWorkflowTransition transitionFrom(
            UUID fromStageIdValue, UUID fromStatusIdValue,
            UUID toStageIdValue, UUID toStatusIdValue
    ) {
        return ServiceWorkflowTransition.create(
                UUID.randomUUID(), versionId, fromStageIdValue, fromStatusIdValue,
                toStageIdValue, toStatusIdValue, "T1", "T1", 1,
                principalId, OffsetDateTime.now());
    }

    private void stubExecutionAndTransition(
            ServiceOrderWorkflowExecution execution,
            ServiceWorkflowTransition transition
    ) {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution));
        when(transitionRepository.findByIdAndWorkflowVersionId(transition.getId(), versionId))
                .thenReturn(Optional.of(transition));
    }

    private void stubValidTransition(
            ServiceOrderWorkflowExecution execution,
            ServiceWorkflowTransition transition
    ) {
        stubExecutionAndTransition(execution, transition);
        when(stageRepository.findByIdAndWorkflowVersionId(
                transition.getToStageId(), versionId)).thenReturn(Optional.of(stage));
        when(stage.isActive()).thenReturn(true);
        when(stage.getId()).thenReturn(transition.getToStageId());
        when(statusRepository.findByIdAndWorkflowStageId(
                transition.getToStatusId(), transition.getToStageId()))
                .thenReturn(Optional.of(workflowStatus));
        when(workflowStatus.isActive()).thenReturn(true);
    }

    private void stubValidAssignment() {
        stubPublishedVersion();
        when(stageRepository.findByIdAndWorkflowVersionId(stageId, versionId))
                .thenReturn(Optional.of(stage));
        when(stage.isActive()).thenReturn(true);
        when(statusRepository.findByIdAndWorkflowStageId(statusId, stageId))
                .thenReturn(Optional.of(workflowStatus));
        when(workflowStatus.isActive()).thenReturn(true);
    }

    private void stubPublishedVersion() {
        stubOrderAndDefinition();
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(version.getWorkflowDefinitionId()).thenReturn(definitionId);
        when(version.getStatus()).thenReturn(ServiceWorkflowVersionStatus.PUBLISHED);
    }

    private void stubOrderAndDefinition() {
        stubOrderAndNoDuplicate();
        when(definitionRepository.findByIdAndTenantId(definitionId, tenantId))
                .thenReturn(Optional.of(definition));
    }

    private void stubOrderAndNoDuplicate() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order));
        when(executionRepository.existsByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(false);
    }

    private int statusOfAssignment() {
        return assertThrows(ResponseStatusException.class, () -> service.assign(
                context, orderId, definitionId, versionId, stageId, statusId))
                .getStatusCode().value();
    }
}