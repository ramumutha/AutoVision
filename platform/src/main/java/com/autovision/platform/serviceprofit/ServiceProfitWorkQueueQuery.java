package com.autovision.platform.serviceprofit;

import java.util.UUID;

public record ServiceProfitWorkQueueQuery(
        ServiceProfitFollowUpHandlingStatus handlingStatus,
        ServiceProfitWorkQueueOwnership ownership,
        ServiceProfitWorkQueueDueState dueState,
        ServiceProfitFollowUpDisposition disposition,
        int page,
        int size
) {
    public ServiceProfitWorkQueueQuery {
        if (ownership == null) ownership = ServiceProfitWorkQueueOwnership.ALL;
        if (dueState == null) dueState = ServiceProfitWorkQueueDueState.ALL;
        if (page < 0) throw new IllegalArgumentException("page must be greater than or equal to zero");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        if (handlingStatus == null) handlingStatus = ServiceProfitFollowUpHandlingStatus.OPEN;
    }

    public long offset() {
        return (long) page * size;
    }
}