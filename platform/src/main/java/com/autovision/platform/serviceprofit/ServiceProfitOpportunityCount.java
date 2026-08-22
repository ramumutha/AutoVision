package com.autovision.platform.serviceprofit;

public record ServiceProfitOpportunityCount(
        String key,
        long count
) {
    public ServiceProfitOpportunityCount {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Summary count key is required"
            );
        }

        if (count < 0) {
            throw new IllegalArgumentException(
                    "Summary count must not be negative"
            );
        }

        key = key.trim();
    }
}