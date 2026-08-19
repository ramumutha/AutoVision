package com.autovision.platform.workflow;

import java.util.UUID;

public record ServiceWorkflowStatusResponse(
        UUID id,
        UUID workflowStageId,
        String code,
        String displayName,
        int sequence,
        boolean active
) {
    public static ServiceWorkflowStatusResponse from(
            ServiceWorkflowStatus status
    ) {
        return new ServiceWorkflowStatusResponse(
                status.getId(), status.getWorkflowStageId(), status.getCode(),
                status.getDisplayName(), status.getSequence(), status.isActive());
    }
}