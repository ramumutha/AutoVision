package com.autovision.platform.serviceprofit.data;

import java.math.BigDecimal;

public record DealerDataCoverage(
        BigDecimal identity,
        BigDecimal vehicleLinkage,
        BigDecimal serviceTransaction,
        BigDecimal recommendationEvidence,
        BigDecimal disposition,
        BigDecimal mileage,
        BigDecimal invoiceLinkage,
        BigDecimal cost
) {

    public DealerDataCoverage {
        validate("identity", identity);
        validate("vehicleLinkage", vehicleLinkage);
        validate("serviceTransaction", serviceTransaction);
        validate("recommendationEvidence", recommendationEvidence);
        validate("disposition", disposition);
        validate("mileage", mileage);
        validate("invoiceLinkage", invoiceLinkage);
        validate("cost", cost);
    }

    private static void validate(
            String name,
            BigDecimal value
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    name + " coverage is required"
            );
        }

        if (value.compareTo(BigDecimal.ZERO) < 0 ||
                value.compareTo(
                        new BigDecimal("100.00")
                ) > 0) {
            throw new IllegalArgumentException(
                    name + " coverage must be between 0 and 100"
            );
        }
    }
}
