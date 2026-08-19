package com.autovision.platform.workflow;

public record CreateServiceWorkflowStatusRequest(
        String code,
        String displayName,
        int sequence
) {
}