package com.autovision.platform.serviceprofit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceProfitFollowUpHistoryResponse(
        ServiceProfitFollowUpEventType eventType,
        ServiceProfitFollowUpHistoryActorType actorType,
        String actorIdentity,
        String applicationIdentity,
        OffsetDateTime occurredAt,
        String previousValue,
        String newValue
) {
    public static ServiceProfitFollowUpHistoryResponse from(ServiceProfitFollowUpHistory history, UUID principalId) {
        String actorIdentity = history.getActorType() == ServiceProfitFollowUpHistoryActorType.SYSTEM
                ? ServiceProfitFollowUpAuditApplication.AUTOVISION_SERVICE_PROFIT
                : history.getActorPrincipalId().equals(principalId) ? "ME" : "HUMAN";
        String applicationIdentity = history.getActorType() == ServiceProfitFollowUpHistoryActorType.SYSTEM
                ? ServiceProfitFollowUpAuditApplication.AUTOVISION_SERVICE_PROFIT : null;
        return new ServiceProfitFollowUpHistoryResponse(history.getEventType(), history.getActorType(), actorIdentity,
                applicationIdentity, history.getOccurredAt(), history.getPreviousValue(), history.getNewValue());
    }
}