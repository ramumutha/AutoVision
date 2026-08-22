package com.autovision.platform.serviceprofit;

import java.util.List;

public record ServiceProfitOpportunitySummary(
        long totalOpportunities,
        long highPriorityCount,
        long reviewRequiredCount,
        long readyCount,
        long suppressedCount,
        List<ServiceProfitCurrencyPotential> potentialByCurrency,
        List<ServiceProfitOpportunityCount> byOpportunityType,
        List<ServiceProfitOpportunityCount> byPriority,
        List<ServiceProfitOpportunityCount> byActionability
) {
    public ServiceProfitOpportunitySummary {
        if (totalOpportunities < 0
                || highPriorityCount < 0
                || reviewRequiredCount < 0
                || readyCount < 0
                || suppressedCount < 0) {
            throw new IllegalArgumentException(
                    "Summary counts must not be negative"
            );
        }

        potentialByCurrency =
                potentialByCurrency == null
                        ? List.of()
                        : List.copyOf(potentialByCurrency);

        byOpportunityType =
                byOpportunityType == null
                        ? List.of()
                        : List.copyOf(byOpportunityType);

        byPriority =
                byPriority == null
                        ? List.of()
                        : List.copyOf(byPriority);

        byActionability =
                byActionability == null
                        ? List.of()
                        : List.copyOf(byActionability);
    }
}