package com.autovision.platform.serviceprofit;

import java.util.List;

public record ServiceProfitWorkQueuePage(
        List<ServiceProfitWorkQueueItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public ServiceProfitWorkQueuePage {
        items = List.copyOf(items);
    }

    public static ServiceProfitWorkQueuePage of(
            List<ServiceProfitWorkQueueItem> items,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new ServiceProfitWorkQueuePage(items, page, size, totalElements, totalPages);
    }
}