package com.autovision.platform.serviceprofit.data;

public record DealerDataCapabilityResult(
        DealerDataCapability capability,
        DealerDataCapabilityStatus status,
        String reason
) {

    public DealerDataCapabilityResult {
        if (capability == null) {
            throw new IllegalArgumentException(
                    "capability is required"
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "capability status is required"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "capability reason is required"
            );
        }
    }
}
