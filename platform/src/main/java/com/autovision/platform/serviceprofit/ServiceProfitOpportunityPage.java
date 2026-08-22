package com.autovision.platform.serviceprofit;

import java.util.List;

public record ServiceProfitOpportunityPage(
        List<ServiceProfitOpportunityQueueItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public ServiceProfitOpportunityPage {
        items = List.copyOf(items);
    }

    public static ServiceProfitOpportunityPage of(
            List<ServiceProfitOpportunityQueueItem> items,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = totalElements == 0
                ? 0
                : (int) ((totalElements + size - 1) / size);

        return new ServiceProfitOpportunityPage(
                items,
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
