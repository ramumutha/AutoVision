package com.autovision.platform.aftersales;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceLineMutationResponse(
        UUID id,
        UUID serviceOrderId,
        UUID serviceJobId,
        int lineNumber,
        ServiceLineType lineType,
        String description,
        BigDecimal quantity,
        String unitOfMeasure,
        long version,
        UUID createdByPrincipalId,
        UUID updatedByPrincipalId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ServiceLineMutationResponse from(
            ServiceLine line
    ) {
        if (line == null) {
            throw new IllegalArgumentException(
                    "Service line is required"
            );
        }

        return new ServiceLineMutationResponse(
                line.getId(),
                line.getServiceOrderId(),
                line.getServiceJobId(),
                line.getLineNumber(),
                line.getLineType(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitOfMeasure(),
                line.getVersion(),
                line.getCreatedByPrincipalId(),
                line.getUpdatedByPrincipalId(),
                line.getCreatedAt(),
                line.getUpdatedAt()
        );
    }
}