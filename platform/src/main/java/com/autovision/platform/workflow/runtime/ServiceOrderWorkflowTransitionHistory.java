package com.autovision.platform.workflow.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Append-only record of one completed runtime workflow movement.
 * Never mutated; corrections are modeled as additional records, not edits.
 */
@Entity
@Table(name = "service_order_workflow_transition_histories", schema = "platform")
public class ServiceOrderWorkflowTransitionHistory {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "workflow_execution_id", nullable = false)
    private UUID workflowExecutionId;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;

    @Column(name = "from_stage_id", nullable = false)
    private UUID fromStageId;

    @Column(name = "from_status_id", nullable = false)
    private UUID fromStatusId;

    @Column(name = "to_stage_id", nullable = false)
    private UUID toStageId;

    @Column(name = "to_status_id", nullable = false)
    private UUID toStatusId;

    @Column(name = "executed_by_principal_id", nullable = false)
    private UUID executedByPrincipalId;

    @Column(name = "executed_at", nullable = false)
    private OffsetDateTime executedAt;

    protected ServiceOrderWorkflowTransitionHistory() {
    }

    public static ServiceOrderWorkflowTransitionHistory record(
            UUID id,
            UUID tenantId,
            UUID serviceOrderId,
            UUID workflowExecutionId,
            UUID workflowDefinitionId,
            UUID workflowVersionId,
            UUID transitionId,
            UUID fromStageId,
            UUID fromStatusId,
            UUID toStageId,
            UUID toStatusId,
            UUID principalId,
            OffsetDateTime executedAt
    ) {
        requireId(id, "Workflow transition history ID");
        requireId(tenantId, "Tenant ID");
        requireId(serviceOrderId, "Service order ID");
        requireId(workflowExecutionId, "Workflow execution ID");
        requireId(workflowDefinitionId, "Workflow definition ID");
        requireId(workflowVersionId, "Workflow version ID");
        requireId(transitionId, "Workflow transition ID");
        requireId(fromStageId, "From workflow stage ID");
        requireId(fromStatusId, "From workflow status ID");
        requireId(toStageId, "To workflow stage ID");
        requireId(toStatusId, "To workflow status ID");
        requireId(principalId, "Principal ID");
        if (executedAt == null) {
            throw new IllegalArgumentException(
                    "Workflow transition history executed time is required");
        }
        if (fromStageId.equals(toStageId) && fromStatusId.equals(toStatusId)) {
            throw new IllegalArgumentException(
                    "Workflow transition history must record a stage or status change");
        }

        ServiceOrderWorkflowTransitionHistory history =
                new ServiceOrderWorkflowTransitionHistory();
        history.id = id;
        history.tenantId = tenantId;
        history.serviceOrderId = serviceOrderId;
        history.workflowExecutionId = workflowExecutionId;
        history.workflowDefinitionId = workflowDefinitionId;
        history.workflowVersionId = workflowVersionId;
        history.transitionId = transitionId;
        history.fromStageId = fromStageId;
        history.fromStatusId = fromStatusId;
        history.toStageId = toStageId;
        history.toStatusId = toStatusId;
        history.executedByPrincipalId = principalId;
        history.executedAt = executedAt;
        return history;
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

    public UUID getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public UUID getTransitionId() {
        return transitionId;
    }

    public UUID getFromStageId() {
        return fromStageId;
    }

    public UUID getFromStatusId() {
        return fromStatusId;
    }

    public UUID getToStageId() {
        return toStageId;
    }

    public UUID getToStatusId() {
        return toStatusId;
    }

    public UUID getExecutedByPrincipalId() {
        return executedByPrincipalId;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    private static void requireId(UUID value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
