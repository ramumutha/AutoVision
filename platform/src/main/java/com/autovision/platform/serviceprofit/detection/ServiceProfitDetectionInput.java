package com.autovision.platform.serviceprofit.detection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceProfitDetectionInput(
        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        UUID locationId,

        UUID customerId,
        UUID vehicleId,

        String sourceSystem,
        String sourceEntityType,
        String sourceEntityId,

        UUID sourceServiceOrderId,
        UUID sourceServiceJobId,
        UUID sourceServiceLineId,
        UUID sourceQuoteId,

        ServiceProfitDisposition disposition,
        ServiceProfitRecommendationStatus recommendationStatus,

        String recommendationText,
        String advisorNote,

        LocalDate recommendationDate,
        LocalDate deferredUntilDate,
        LocalDate nextServiceDueDate,
        LocalDate lastServiceDate,

        BigDecimal currentMileage,
        BigDecimal nextServiceDueMileage,

        OffsetDateTime lastCustomerActivityAt,

        BigDecimal recommendedAmount,
        BigDecimal invoicedAmount,
        BigDecimal attributableCost,
        String currencyCode,

        boolean customerContactable,
        boolean completedWorkEvidence,
        boolean invoiceEvidence,

        List<ServiceProfitEvidenceRef> evidence
) {
    public ServiceProfitDetectionInput {

        if (tenantId == null) {
            throw new IllegalArgumentException(
                    "Detection input tenant ID is required"
            );
        }

        requireText(
                sourceSystem,
                "Detection input source system is required"
        );

        requireText(
                sourceEntityType,
                "Detection input source entity type is required"
        );

        requireText(
                sourceEntityId,
                "Detection input source entity ID is required"
        );

        sourceSystem = sourceSystem.trim();
        sourceEntityType = sourceEntityType.trim();
        sourceEntityId = sourceEntityId.trim();

        recommendationText =
                normalizeOptionalText(recommendationText);

        advisorNote =
                normalizeOptionalText(advisorNote);

        currencyCode =
                normalizeOptionalText(currencyCode);

        if (currencyCode != null) {
            currencyCode = currencyCode.toUpperCase();

            if (currencyCode.length() != 3) {
                throw new IllegalArgumentException(
                        "Detection input currency code must contain exactly 3 characters"
                );
            }
        }

        requireNonNegative(
                currentMileage,
                "Current mileage must not be negative"
        );

        requireNonNegative(
                nextServiceDueMileage,
                "Next service due mileage must not be negative"
        );

        requireNonNegative(
                recommendedAmount,
                "Recommended amount must not be negative"
        );

        requireNonNegative(
                invoicedAmount,
                "Invoiced amount must not be negative"
        );

        requireNonNegative(
                attributableCost,
                "Attributable cost must not be negative"
        );

        if (recommendedAmount != null && currencyCode == null) {
            throw new IllegalArgumentException(
                    "Currency is required when recommended amount is provided"
            );
        }

        if (invoicedAmount != null && currencyCode == null) {
            throw new IllegalArgumentException(
                    "Currency is required when invoiced amount is provided"
            );
        }

        if (attributableCost != null && currencyCode == null) {
            throw new IllegalArgumentException(
                    "Currency is required when attributable cost is provided"
            );
        }

        evidence = evidence == null
                ? List.of()
                : List.copyOf(evidence);
    }

    private static void requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String normalizeOptionalText(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static void requireNonNegative(
            BigDecimal value,
            String message
    ) {
        if (value != null &&
                value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
