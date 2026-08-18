package com.autovision.platform.aftersales;

import java.util.UUID;

public record CreateServiceOrderRequest(
        String orderNumber,
        UUID dealerId,
        UUID branchId,
        UUID vehicleId
) {
}