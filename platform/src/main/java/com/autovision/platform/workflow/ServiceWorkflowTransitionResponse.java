package com.autovision.platform.workflow;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceWorkflowTransitionResponse(
        UUID id,
        UUID workflowVersionId,
        UUID fromStageId,
        UUID fromStatusId,
        UUID toStageId,
        UUID toStatusId,
        String code,
        String displayName,
        int sequence,
        boolean active,
        UUID createdByPrincipalId,
        UUID updatedByPrincipalId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ServiceWorkflowTransitionResponse from(
            ServiceWorkflowTransition transition
    ) {
        return new ServiceWorkflowTransitionResponse(
                transition.getId(), transition.getWorkflowVersionId(),
                transition.getFromStageId(), transition.getFromStatusId(),
                transition.getToStageId(), transition.getToStatusId(),
                transition.getCode(), transition.getDisplayName(),
                transition.getSequence(), transition.isActive(),
                transition.getCreatedByPrincipalId(), transition.getUpdatedByPrincipalId(),
                transition.getCreatedAt(), transition.getUpdatedAt());
    }
}
