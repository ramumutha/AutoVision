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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceOrderWorkflowExecutionCommandService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceOrderWorkflowExecutionRepository executionRepository;
    private final ServiceWorkflowDefinitionRepository definitionRepository;
    private final ServiceWorkflowVersionRepository versionRepository;
    private final ServiceWorkflowStageRepository stageRepository;
    private final ServiceWorkflowStatusRepository statusRepository;
    private final AuthorizationService authorizationService;

    public ServiceOrderWorkflowExecutionCommandService(
            ServiceOrderRepository orderRepository,
            ServiceOrderWorkflowExecutionRepository executionRepository,
            ServiceWorkflowDefinitionRepository definitionRepository,
            ServiceWorkflowVersionRepository versionRepository,
            ServiceWorkflowStageRepository stageRepository,
            ServiceWorkflowStatusRepository statusRepository,
            AuthorizationService authorizationService
    ) {
        this.orderRepository = orderRepository;
        this.executionRepository = executionRepository;
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.stageRepository = stageRepository;
        this.statusRepository = statusRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ServiceOrderWorkflowExecution assign(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId,
            UUID workflowDefinitionId,
            UUID workflowVersionId,
            UUID initialStageId,
            UUID initialStatusId
    ) {
        requireInputs(tenantContext, serviceOrderId, workflowDefinitionId,
                workflowVersionId, initialStageId, initialStatusId);

        authorizationService.requirePermission(new AuthorizationRequest(
                tenantContext,
                AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));

        ServiceOrder order = orderRepository.findByIdAndTenantId(
                        serviceOrderId, tenantContext.tenantId())
                .orElseThrow(() -> notFound("Service order not found"));

        if (executionRepository.existsByServiceOrderIdAndTenantId(
                serviceOrderId, tenantContext.tenantId())) {
            throw conflict("Service order already has a workflow execution");
        }

        ServiceWorkflowDefinition definition = definitionRepository
                .findByIdAndTenantId(workflowDefinitionId, tenantContext.tenantId())
                .orElseThrow(() -> notFound("Workflow definition not found"));

        authorizationService.requirePermission(new AuthorizationRequest(
                tenantContext,
                ServiceWorkflowPermissions.READ,
                AuthorizationResourceType.SERVICE_WORKFLOW,
                workflowDefinitionId
        ));

        ServiceWorkflowVersion version = versionRepository.findById(workflowVersionId)
                .filter(candidate -> workflowDefinitionId.equals(
                        candidate.getWorkflowDefinitionId()))
                .orElseThrow(() -> notFound("Workflow version not found"));
        if (version.getStatus() != ServiceWorkflowVersionStatus.PUBLISHED) {
            throw badRequest("Only published workflow versions can be assigned");
        }

        ServiceWorkflowStage stage = stageRepository.findByIdAndWorkflowVersionId(
                        initialStageId, workflowVersionId)
                .orElseThrow(() -> notFound("Workflow stage not found"));
        if (!stage.isActive()) {
            throw badRequest("Workflow stage is inactive");
        }

        ServiceWorkflowStatus status = statusRepository.findByIdAndWorkflowStageId(
                        initialStatusId, initialStageId)
                .orElseThrow(() -> notFound("Workflow status not found"));
        if (!status.isActive()) {
            throw badRequest("Workflow status is inactive");
        }

        return executionRepository.save(ServiceOrderWorkflowExecution.start(
                UUID.randomUUID(),
                tenantContext.tenantId(),
                serviceOrderId,
                workflowDefinitionId,
                workflowVersionId,
                initialStageId,
                initialStatusId,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        ));
    }

    private void requireInputs(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId,
            UUID workflowDefinitionId,
            UUID workflowVersionId,
            UUID initialStageId,
            UUID initialStatusId
    ) {
        if (tenantContext == null || tenantContext.tenantId() == null
                || tenantContext.userRefId() == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Authenticated tenant context is required");
        }
        requireId(serviceOrderId, "Service order ID");
        requireId(workflowDefinitionId, "Workflow definition ID");
        requireId(workflowVersionId, "Workflow version ID");
        requireId(initialStageId, "Initial workflow stage ID");
        requireId(initialStatusId, "Initial workflow status ID");
    }

    private void requireId(UUID value, String label) {
        if (value == null) {
            throw new ResponseStatusException(BAD_REQUEST, label + " is required");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(CONFLICT, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }
}