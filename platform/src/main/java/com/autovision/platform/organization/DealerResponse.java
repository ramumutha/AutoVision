package com.autovision.platform.organization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DealerResponse(
        UUID id,
        String code,
        String name,
        String legalName,
        UUID primaryLocationId,
        OrganizationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}