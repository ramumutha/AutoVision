package com.autovision.platform.serviceprofit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceProfitWorkQueueItemResponse(
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
    public static ServiceProfitWorkQueueItemResponse from(ServiceProfitWorkQueueItem item) {
        return new ServiceProfitWorkQueueItemResponse(
                item.followUpId(), item.opportunityId(), item.handlingStatus(), item.ownerPrincipalId(),
                item.claimedAt(), item.dueAt(), item.disposition(), item.version(), item.createdAt(),
                item.updatedAt(), item.actionability(), item.opportunityStatus(), item.evidenceClass(),
                item.evidenceStrength(), item.priority(), item.title(), item.summary());
    }
}