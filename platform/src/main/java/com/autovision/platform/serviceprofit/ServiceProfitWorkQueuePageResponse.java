package com.autovision.platform.serviceprofit;

import java.util.List;

public record ServiceProfitWorkQueuePageResponse(
        List<ServiceProfitWorkQueueItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public ServiceProfitWorkQueuePageResponse {
        items = List.copyOf(items);
    }

    public static ServiceProfitWorkQueuePageResponse from(ServiceProfitWorkQueuePage page) {
        return new ServiceProfitWorkQueuePageResponse(
                page.items().stream().map(ServiceProfitWorkQueueItemResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}