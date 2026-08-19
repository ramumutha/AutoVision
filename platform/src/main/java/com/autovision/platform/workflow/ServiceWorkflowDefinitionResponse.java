package com.autovision.platform.workflow;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceWorkflowDefinitionResponse(
        UUID id,
        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        String code,
        String displayName,
        boolean active,
        long version,
        UUID createdByPrincipalId,
        UUID updatedByPrincipalId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ServiceWorkflowDefinitionResponse from(
            ServiceWorkflowDefinition definition
    ) {
        return new ServiceWorkflowDefinitionResponse(
                definition.getId(), definition.getTenantId(),
                definition.getDealerId(), definition.getBranchId(),
                definition.getCode(), definition.getDisplayName(),
                definition.isActive(), definition.getVersion(),
                definition.getCreatedByPrincipalId(),
                definition.getUpdatedByPrincipalId(),
                definition.getCreatedAt(), definition.getUpdatedAt());
    }
}