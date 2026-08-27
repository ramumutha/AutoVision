package com.autovision.platform.serviceprofit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceProfitWorkQueueItem(
        UUID followUpId,
        UUID opportunityId,
        ServiceProfitFollowUpHandlingStatus handlingStatus,
        UUID ownerPrincipalId,
        OffsetDateTime claimedAt,
        OffsetDateTime dueAt,
        ServiceProfitFollowUpDisposition disposition,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        ServiceProfitActionability actionability,
        ServiceProfitOpportunityStatus opportunityStatus,
        ServiceProfitEvidenceClass evidenceClass,
        ServiceProfitEvidenceStrength evidenceStrength,
        ServiceProfitPriority priority,
        String title,
        String summary
) {
}