package com.autovision.platform.serviceprofit.detection;

import java.time.OffsetDateTime;

public record ServiceProfitEvidenceRef(
        ServiceProfitEvidenceSourceType sourceType,
        String sourceId,
        OffsetDateTime observedAt
) {
    public ServiceProfitEvidenceRef {
        if (sourceType == null) {
            throw new IllegalArgumentException(
                    "Evidence source type is required"
            );
        }

        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException(
                    "Evidence source ID is required"
            );
        }

        sourceId = sourceId.trim();
    }
}
