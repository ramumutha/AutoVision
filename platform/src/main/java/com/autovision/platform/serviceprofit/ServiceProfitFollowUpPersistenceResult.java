package com.autovision.platform.serviceprofit;

public record ServiceProfitFollowUpPersistenceResult(
        ServiceProfitFollowUp followUp,
        boolean created
) {
    public ServiceProfitFollowUpPersistenceResult {
        if (followUp == null) throw new IllegalArgumentException("Follow-up is required");
    }
}