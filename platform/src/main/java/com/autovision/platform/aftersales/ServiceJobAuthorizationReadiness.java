package com.autovision.platform.aftersales;

import java.util.Objects;
import java.util.UUID;

public record ServiceJobAuthorizationReadiness(
        UUID serviceJobId,
        UUID serviceOrderId,
        boolean ready,
        OperationalAuthorizationStatus operationalAuthorizationStatus,
        ServiceJobAuthorizationReadinessReason reason
) {
    public ServiceJobAuthorizationReadiness {
        Objects.requireNonNull(serviceJobId, "Service job ID is required");
        Objects.requireNonNull(serviceOrderId, "Service order ID is required");
        Objects.requireNonNull(
                operationalAuthorizationStatus,
                "Operational authorization status is required"
        );
        Objects.requireNonNull(reason, "Readiness reason is required");
    }
}
