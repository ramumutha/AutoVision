package com.autovision.platform.serviceprofit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceProfitFollowUpActivationResult(
        UUID opportunityId,
        UUID followUpId,
        boolean created,
        ServiceProfitFollowUpHandlingStatus handlingStatus,
        UUID ownerPrincipalId,
        ServiceProfitFollowUpDisposition disposition,
        OffsetDateTime nextActionDueAt
) {
    public ServiceProfitFollowUpActivationResult {
        if (opportunityId == null || followUpId == null || handlingStatus == null || disposition == null) {
            throw new IllegalArgumentException("Activation result identity and state are required");
        }
    }
}