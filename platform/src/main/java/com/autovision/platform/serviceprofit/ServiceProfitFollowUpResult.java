package com.autovision.platform.serviceprofit;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Internal result model for Service Profit follow-up commands and reads. */
public record ServiceProfitFollowUpResult(
        UUID followUpId,
        UUID opportunityId,
        UUID ownerPrincipalId,
        ServiceProfitFollowUpHandlingStatus handlingStatus,
        OffsetDateTime nextActionDueAt,
        ServiceProfitFollowUpDisposition disposition,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ServiceProfitFollowUpResult from(ServiceProfitFollowUp followUp) {
        if (followUp == null) throw new IllegalArgumentException("Follow-up is required");
        return new ServiceProfitFollowUpResult(
                followUp.getId(), followUp.getOpportunityId(), followUp.getOwnerPrincipalId(),
                followUp.getHandlingStatus(), followUp.getNextActionDueAt(),
                followUp.getCurrentDisposition(), followUp.getVersion(),
                followUp.getCreatedAt(), followUp.getUpdatedAt());
    }
}