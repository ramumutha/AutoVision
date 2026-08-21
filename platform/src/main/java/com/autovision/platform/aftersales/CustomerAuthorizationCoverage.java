package com.autovision.platform.aftersales;

import java.util.List;
import java.util.UUID;

public record CustomerAuthorizationCoverage(
        UUID authorizationId,
        UUID serviceQuoteId,
        UUID serviceOrderId,
        List<UUID> serviceLineIds,
        List<UUID> serviceJobIds
) {
    public CustomerAuthorizationCoverage {
        serviceLineIds = List.copyOf(serviceLineIds);
        serviceJobIds = List.copyOf(serviceJobIds);
    }
}
