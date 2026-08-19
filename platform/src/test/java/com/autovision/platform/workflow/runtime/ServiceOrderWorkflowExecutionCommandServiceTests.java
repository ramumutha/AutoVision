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
import com.autovision.platform.workflow.ServiceWorkflowVersion;
import com.autovision.platform.workflow.ServiceWorkflowVersionRepository;
import com.autovision.platform.workflow.ServiceWorkflowVersionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                authorizationService);
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