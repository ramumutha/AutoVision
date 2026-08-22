package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunity;

public record ServiceProfitPersistenceResult(
        ServiceProfitPersistenceOutcome outcome,
        ServiceProfitOpportunity opportunity
) {

    public ServiceProfitPersistenceResult {
        if (outcome == null) {
            throw new IllegalArgumentException(
                    "Persistence outcome is required"
            );
        }

        if (outcome != ServiceProfitPersistenceOutcome.NO_MATCH
                && opportunity == null) {
            throw new IllegalArgumentException(
                    "Opportunity is required for persistence outcome"
            );
        }

        if (outcome == ServiceProfitPersistenceOutcome.NO_MATCH
                && opportunity != null) {
            throw new IllegalArgumentException(
                    "No-match persistence result cannot contain opportunity"
            );
        }
    }

    public static ServiceProfitPersistenceResult noMatch() {
        return new ServiceProfitPersistenceResult(
                ServiceProfitPersistenceOutcome.NO_MATCH,
                null
        );
    }
}