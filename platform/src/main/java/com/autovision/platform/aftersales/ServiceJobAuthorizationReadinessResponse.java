package com.autovision.platform.aftersales;

import java.util.UUID;

public record ServiceJobAuthorizationReadinessResponse(
        UUID serviceJobId,
        UUID serviceOrderId,
        boolean ready,
        OperationalAuthorizationStatus operationalAuthorizationStatus,
        ServiceJobAuthorizationReadinessReason reason
) {
}
