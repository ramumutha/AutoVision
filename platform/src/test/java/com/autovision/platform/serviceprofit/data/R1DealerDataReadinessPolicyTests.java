package com.autovision.platform.serviceprofit.data;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class R1DealerDataReadinessPolicyTests {

    private final R1DealerDataReadinessPolicy policy =
            new R1DealerDataReadinessPolicy();

    @Test
    void calculatesWeightedExplainableScore() {
        DealerDataCoverage coverage =
                new DealerDataCoverage(
                        new BigDecimal("95.00"),
                        new BigDecimal("94.00"),
                        new BigDecimal("98.00"),
                        new BigDecimal("82.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("71.00"),
                        new BigDecimal("93.00"),
                        new BigDecimal("35.00")
                );

        assertEquals(
                new BigDecimal("70.45"),
                policy.calculateOverallScore(coverage)
        );

        assertEquals(
                "R1-DATA-READINESS-1",
                policy.policyVersion()
        );
    }

    @Test
    void perfectCoverageScoresOneHundred() {
        BigDecimal hundred =
                new BigDecimal("100.00");

        DealerDataCoverage coverage =
                new DealerDataCoverage(
                        hundred,
                        hundred,
                        hundred,
                        hundred,
                        hundred,
                        hundred,
                        hundred,
                        hundred
                );

        assertEquals(
                new BigDecimal("100.00"),
                policy.calculateOverallScore(coverage)
        );
    }

    @Test
    void coverageRejectsValuesAboveOneHundred() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DealerDataCoverage(
                        new BigDecimal("101.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
        );
    }
}
