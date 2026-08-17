package com.autovision.platform.aftersales;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AfterSalesCaseResponse(
        UUID id,
        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        String caseNumber,
        AfterSalesCaseStatus lifecycleStatus,
        AfterSalesCaseSourceChannel sourceChannel,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}