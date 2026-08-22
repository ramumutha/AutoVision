package com.autovision.platform.serviceprofit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceProfitOpportunityQueueItem(
        UUID id,
        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        UUID locationId,
        String opportunityKey,
        ServiceProfitOpportunityType opportunityType,
        ServiceProfitOpportunityStatus status,
        ServiceProfitEvidenceClass evidenceClass,
        ServiceProfitEvidenceStrength evidenceStrength,
        ServiceProfitPriority priority,
        ServiceProfitActionability actionability,
        String title,
        BigDecimal potentialAmount,
        String currencyCode,
        OffsetDateTime detectedAt
) {
}
