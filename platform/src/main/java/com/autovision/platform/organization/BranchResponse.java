package com.autovision.platform.organization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        UUID dealerId,
        UUID locationId,
        String code,
        String name,
        OrganizationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}