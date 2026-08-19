package com.autovision.platform.workflow;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateServiceWorkflowTransitionRequest(
        @NotNull UUID fromStageId,
        @NotNull UUID fromStatusId,
        @NotNull UUID toStageId,
        @NotNull UUID toStatusId,
        @NotBlank String code,
        @NotBlank String displayName,
        @Min(0) int sequence
) {
}
