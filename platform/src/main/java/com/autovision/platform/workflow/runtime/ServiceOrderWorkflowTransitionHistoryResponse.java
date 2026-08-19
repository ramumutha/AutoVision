package com.autovision.platform.workflow.runtime;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceOrderWorkflowTransitionHistoryResponse(
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
        UUID executedByPrincipalId,
        OffsetDateTime executedAt
) {
    public static ServiceOrderWorkflowTransitionHistoryResponse from(
            ServiceOrderWorkflowTransitionHistory history
    ) {
        return new ServiceOrderWorkflowTransitionHistoryResponse(
                history.getId(),
                history.getTenantId(),
                history.getServiceOrderId(),
                history.getWorkflowExecutionId(),
                history.getWorkflowDefinitionId(),
                history.getWorkflowVersionId(),
                history.getTransitionId(),
                history.getFromStageId(),
                history.getFromStatusId(),
                history.getToStageId(),
                history.getToStatusId(),
                history.getExecutedByPrincipalId(),
                history.getExecutedAt()
        );
    }
}
