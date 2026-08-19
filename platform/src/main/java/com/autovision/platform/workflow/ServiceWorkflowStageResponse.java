package com.autovision.platform.workflow;

import java.util.UUID;

public record ServiceWorkflowStageResponse(
        UUID id,
        UUID workflowVersionId,
        String code,
        String displayName,
        int sequence,
        boolean active
) {
    public static ServiceWorkflowStageResponse from(
            ServiceWorkflowStage stage
    ) {
        return new ServiceWorkflowStageResponse(
                stage.getId(), stage.getWorkflowVersionId(), stage.getCode(),
                stage.getDisplayName(), stage.getSequence(), stage.isActive());
    }
}