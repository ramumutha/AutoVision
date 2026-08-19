package com.autovision.platform.workflow.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Runtime pin for the published workflow configuration assigned to an order.
 * Configuration codes and names remain outside this aggregate.
 */
@Entity
@Table(name = "service_order_workflow_executions", schema = "platform")
public class ServiceOrderWorkflowExecution {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "current_stage_id", nullable = false)
    private UUID currentStageId;

    @Column(name = "current_status_id", nullable = false)
    private UUID currentStatusId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_by_principal_id", nullable = false)
    private UUID createdByPrincipalId;

    @Column(name = "updated_by_principal_id", nullable = false)
    private UUID updatedByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ServiceOrderWorkflowExecution() {
    }

    public static ServiceOrderWorkflowExecution start(
            UUID id,
            UUID tenantId,
            UUID serviceOrderId,
            UUID workflowDefinitionId,
            UUID workflowVersionId,
            UUID initialStageId,
            UUID initialStatusId,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(id, "Workflow execution ID");
        requireId(tenantId, "Tenant ID");
        requireId(serviceOrderId, "Service order ID");
        requireId(workflowDefinitionId, "Workflow definition ID");
        requireId(workflowVersionId, "Workflow version ID");
        requireId(initialStageId, "Initial workflow stage ID");
        requireId(initialStatusId, "Initial workflow status ID");
        requireId(principalId, "Principal ID");
        if (now == null) {
            throw new IllegalArgumentException(
                    "Workflow execution timestamp is required");
        }

        ServiceOrderWorkflowExecution execution =
                new ServiceOrderWorkflowExecution();
        execution.id = id;
        execution.tenantId = tenantId;
        execution.serviceOrderId = serviceOrderId;
        execution.workflowDefinitionId = workflowDefinitionId;
        execution.workflowVersionId = workflowVersionId;
        execution.currentStageId = initialStageId;
        execution.currentStatusId = initialStatusId;
        execution.createdByPrincipalId = principalId;
        execution.updatedByPrincipalId = principalId;
        execution.createdAt = now;
        execution.updatedAt = now;
        return execution;
    }

    public void moveTo(
            UUID targetStageId,
            UUID targetStatusId,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(targetStageId, "Target workflow stage ID");
        requireId(targetStatusId, "Target workflow status ID");
        requireId(principalId, "Principal ID");
        if (now == null) {
            throw new IllegalArgumentException(
                    "Workflow execution timestamp is required");
        }
        if (targetStageId.equals(currentStageId)
                && targetStatusId.equals(currentStatusId)) {
            throw new IllegalStateException(
                    "Workflow execution is already at the target stage and status");
        }

        currentStageId = targetStageId;
        currentStatusId = targetStatusId;
        updatedByPrincipalId = principalId;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public UUID getCurrentStageId() {
        return currentStageId;
    }

    public UUID getCurrentStatusId() {
        return currentStatusId;
    }

    public long getVersion() {
        return version;
    }

    public UUID getCreatedByPrincipalId() {
        return createdByPrincipalId;
    }

    public UUID getUpdatedByPrincipalId() {
        return updatedByPrincipalId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    private static void requireId(UUID value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}