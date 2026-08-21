package com.autovision.platform.aftersales;

import java.util.UUID;
import java.util.Objects;

public record OperationalAuthorizationEvaluation(
        UUID serviceJobId,
        UUID serviceOrderId,
        OperationalAuthorizationStatus status,
        int totalLineCount,
        int authorizedLineCount,
        int pendingLineCount,
        int notAuthorizedLineCount
) {
    public OperationalAuthorizationEvaluation {
        Objects.requireNonNull(serviceJobId, "Service job ID is required");
        Objects.requireNonNull(serviceOrderId, "Service order ID is required");
        Objects.requireNonNull(status, "Operational authorization status is required");
        if (totalLineCount < 0
                || authorizedLineCount < 0
                || pendingLineCount < 0
                || notAuthorizedLineCount < 0) {
            throw new IllegalArgumentException(
                    "Operational authorization counts must not be negative"
            );
        }
        if (status != OperationalAuthorizationStatus.NOT_REQUIRED
                && authorizedLineCount
                        + pendingLineCount
                        + notAuthorizedLineCount != totalLineCount) {
            throw new IllegalArgumentException(
                    "Operational authorization counts are inconsistent"
            );
        }
    }
}
