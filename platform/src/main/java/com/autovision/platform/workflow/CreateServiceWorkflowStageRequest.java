package com.autovision.platform.workflow;

public record CreateServiceWorkflowStageRequest(
        String code,
        String displayName,
        int sequence
) {
}