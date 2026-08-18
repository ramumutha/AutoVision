package com.autovision.platform.aftersales;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceOrderMutationResponse(
        UUID id,
        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        String orderNumber,
        UUID vehicleId,
        ServiceOrderStatus status,
        OffsetDateTime openedAt,
        OffsetDateTime completedAt,
        OffsetDateTime closedAt,
        OffsetDateTime cancelledAt,
        long version,
        UUID createdByPrincipalId,
        UUID updatedByPrincipalId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ServiceOrderMutationResponse from(
            ServiceOrder order
    ) {
        if (order == null) {
            throw new IllegalArgumentException(
                    "Service order is required"
            );
        }

        return new ServiceOrderMutationResponse(
                order.getId(),
                order.getTenantId(),
                order.getDealerId(),
                order.getBranchId(),
                order.getOrderNumber(),
                order.getVehicleId(),
                order.getStatus(),
                order.getOpenedAt(),
                order.getCompletedAt(),
                order.getClosedAt(),
                order.getCancelledAt(),
                order.getVersion(),
                order.getCreatedByPrincipalId(),
                order.getUpdatedByPrincipalId(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}