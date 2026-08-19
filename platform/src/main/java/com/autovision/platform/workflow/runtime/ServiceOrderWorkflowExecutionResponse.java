package com.autovision.platform.workflow.runtime;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceOrderWorkflowExecutionResponse(
        UUID id,
        UUID tenantId,
        UUID serviceOrderId,
        UUID workflowDefinitionId,
        UUID workflowVersionId,
        UUID currentStageId,
        UUID currentStatusId,
        UUID createdByPrincipalId,
        UUID updatedByPrincipalId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ServiceOrderWorkflowExecutionResponse from(
            ServiceOrderWorkflowExecution execution
    ) {
        return new ServiceOrderWorkflowExecutionResponse(
                execution.getId(),
                execution.getTenantId(),
                execution.getServiceOrderId(),
                execution.getWorkflowDefinitionId(),
                execution.getWorkflowVersionId(),
                execution.getCurrentStageId(),
                execution.getCurrentStatusId(),
                execution.getCreatedByPrincipalId(),
                execution.getUpdatedByPrincipalId(),
                execution.getCreatedAt(),
                execution.getUpdatedAt()
        );
    }
}