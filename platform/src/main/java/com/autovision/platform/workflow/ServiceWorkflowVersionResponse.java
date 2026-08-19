package com.autovision.platform.workflow;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceWorkflowVersionResponse(
        UUID id,
        UUID workflowDefinitionId,
        long versionNumber,
        ServiceWorkflowVersionStatus status,
        UUID createdByPrincipalId,
        OffsetDateTime createdAt,
        UUID publishedByPrincipalId,
        OffsetDateTime publishedAt
) {
    public static ServiceWorkflowVersionResponse from(
            ServiceWorkflowVersion version
    ) {
        return new ServiceWorkflowVersionResponse(
                version.getId(), version.getWorkflowDefinitionId(),
                version.getVersionNumber(), version.getStatus(),
                version.getCreatedByPrincipalId(), version.getCreatedAt(),
                version.getPublishedByPrincipalId(), version.getPublishedAt());
    }
}