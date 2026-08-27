package com.autovision.platform.serviceprofit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceProfitFollowUpResponse(
        UUID opportunityId,
        ServiceProfitFollowUpHandlingStatus handlingStatus,
        ServiceProfitFollowUpOwnership ownership,
        OffsetDateTime dueAt,
        ServiceProfitWorkQueueDueState dueState,
        ServiceProfitFollowUpDisposition disposition,
        long version
) {
    public static ServiceProfitFollowUpResponse from(ServiceProfitFollowUp followUp, UUID principalId, OffsetDateTime now) {
        ServiceProfitWorkQueueDueState dueState = followUp.getNextActionDueAt() == null
                ? ServiceProfitWorkQueueDueState.NO_DUE_DATE
                : followUp.getNextActionDueAt().isBefore(now)
                ? ServiceProfitWorkQueueDueState.OVERDUE : ServiceProfitWorkQueueDueState.UPCOMING;
        ServiceProfitFollowUpOwnership ownership = followUp.getOwnerPrincipalId() == null
                ? ServiceProfitFollowUpOwnership.UNASSIGNED
                : followUp.getOwnerPrincipalId().equals(principalId)
                ? ServiceProfitFollowUpOwnership.MINE : ServiceProfitFollowUpOwnership.ASSIGNED;
        return new ServiceProfitFollowUpResponse(followUp.getOpportunityId(), followUp.getHandlingStatus(), ownership,
                followUp.getNextActionDueAt(), dueState, followUp.getCurrentDisposition(), followUp.getVersion());
    }
}