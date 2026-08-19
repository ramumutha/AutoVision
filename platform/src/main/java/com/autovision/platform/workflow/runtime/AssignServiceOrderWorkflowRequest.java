package com.autovision.platform.workflow.runtime;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignServiceOrderWorkflowRequest(
        @NotNull UUID workflowDefinitionId,
        @NotNull UUID workflowVersionId,
        @NotNull UUID initialStageId,
        @NotNull UUID initialStatusId
) {
}