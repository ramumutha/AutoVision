package com.autovision.platform.serviceprofit.data;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class R1DealerDataReadinessPolicy {

    public static final String POLICY_VERSION =
            "R1-DATA-READINESS-1";

    private static final BigDecimal IDENTITY_WEIGHT =
            new BigDecimal("0.10");

    private static final BigDecimal VEHICLE_LINKAGE_WEIGHT =
            new BigDecimal("0.10");

    private static final BigDecimal SERVICE_TRANSACTION_WEIGHT =
            new BigDecimal("0.15");

    private static final BigDecimal RECOMMENDATION_EVIDENCE_WEIGHT =
            new BigDecimal("0.15");

    private static final BigDecimal DISPOSITION_WEIGHT =
            new BigDecimal("0.15");

    private static final BigDecimal MILEAGE_WEIGHT =
            new BigDecimal("0.10");

    private static final BigDecimal INVOICE_LINKAGE_WEIGHT =
            new BigDecimal("0.15");

    private static final BigDecimal COST_WEIGHT =
            new BigDecimal("0.10");

    public BigDecimal calculateOverallScore(
            DealerDataCoverage coverage
    ) {
        return coverage.identity()
                .multiply(IDENTITY_WEIGHT)
                .add(
                        coverage.vehicleLinkage()
                                .multiply(VEHICLE_LINKAGE_WEIGHT)
                )
                .add(
                        coverage.serviceTransaction()
                                .multiply(SERVICE_TRANSACTION_WEIGHT)
                )
                .add(
                        coverage.recommendationEvidence()
                                .multiply(
                                        RECOMMENDATION_EVIDENCE_WEIGHT
                                )
                )
                .add(
                        coverage.disposition()
                                .multiply(DISPOSITION_WEIGHT)
                )
                .add(
                        coverage.mileage()
                                .multiply(MILEAGE_WEIGHT)
                )
                .add(
                        coverage.invoiceLinkage()
                                .multiply(INVOICE_LINKAGE_WEIGHT)
                )
                .add(
                        coverage.cost()
                                .multiply(COST_WEIGHT)
                )
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    public String policyVersion() {
        return POLICY_VERSION;
    }
}
