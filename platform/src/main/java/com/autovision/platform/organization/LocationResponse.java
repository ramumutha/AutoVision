package com.autovision.platform.organization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String stateProvince,
        String postalCode,
        String countryCode,
        String timezone,
        BigDecimal latitude,
        BigDecimal longitude,
        OrganizationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}