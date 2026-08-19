package com.autovision.platform.workflow;

import java.util.UUID;

public record CreateServiceWorkflowDefinitionRequest(
        UUID dealerId,
        UUID branchId,
        String code,
        String displayName
) {
}