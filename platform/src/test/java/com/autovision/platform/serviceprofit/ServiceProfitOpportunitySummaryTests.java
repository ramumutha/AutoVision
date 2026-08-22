package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceProfitOpportunitySummaryTests {

    @Test
    void preservesManagerSummaryMetrics() {
        ServiceProfitOpportunitySummary summary =
                new ServiceProfitOpportunitySummary(
                        10,
                        4,
                        1,
                        7,
                        1,
                        List.of(
                                new ServiceProfitCurrencyPotential(
                                        "usd",
                                        new BigDecimal("4250.00")
                                )
                        ),
                        List.of(
                                new ServiceProfitOpportunityCount(
                                        "DECLINED_WORK",
                                        3
                                )
                        ),
                        List.of(
                                new ServiceProfitOpportunityCount(
                                        "HIGH",
                                        4
                                )
                        ),
                        List.of(
                                new ServiceProfitOpportunityCount(
                                        "READY",
                                        7
                                )
                        )
                );

        assertEquals(10, summary.totalOpportunities());
        assertEquals(4, summary.highPriorityCount());
        assertEquals(1, summary.reviewRequiredCount());
        assertEquals(7, summary.readyCount());
        assertEquals(1, summary.suppressedCount());

        assertEquals(
                "USD",
                summary.potentialByCurrency()
                        .getFirst()
                        .currencyCode()
        );

        assertEquals(
                0,
                new BigDecimal("4250.00").compareTo(
                        summary.potentialByCurrency()
                                .getFirst()
                                .amount()
                )
        );
    }

    @Test
    void preservesSeparateCurrencyTotals() {
        ServiceProfitOpportunitySummary summary =
                new ServiceProfitOpportunitySummary(
                        2,
                        1,
                        0,
                        2,
                        0,
                        List.of(
                                new ServiceProfitCurrencyPotential(
                                        "USD",
                                        new BigDecimal("100.00")
                                ),
                                new ServiceProfitCurrencyPotential(
                                        "EUR",
                                        new BigDecimal("200.00")
                                )
                        ),
                        List.of(),
                        List.of(),
                        List.of()
                );

        assertEquals(
                2,
                summary.potentialByCurrency().size()
        );

        assertEquals(
                "USD",
                summary.potentialByCurrency()
                        .get(0)
                        .currencyCode()
        );

        assertEquals(
                "EUR",
                summary.potentialByCurrency()
                        .get(1)
                        .currencyCode()
        );
    }

    @Test
    void summaryCollectionsAreImmutableCopies() {
        List<ServiceProfitOpportunityCount> counts =
                new ArrayList<>();

        counts.add(
                new ServiceProfitOpportunityCount(
                        "HIGH",
                        1
                )
        );

        ServiceProfitOpportunitySummary summary =
                new ServiceProfitOpportunitySummary(
                        1,
                        1,
                        0,
                        1,
                        0,
                        List.of(),
                        List.of(),
                        counts,
                        List.of()
                );

        counts.add(
                new ServiceProfitOpportunityCount(
                        "LOW",
                        1
                )
        );

        assertEquals(
                1,
                summary.byPriority().size()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> summary.byPriority().add(
                        new ServiceProfitOpportunityCount(
                                "MEDIUM",
                                1
                        )
                )
        );
    }

    @Test
    void rejectsInvalidContractValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceProfitOpportunityCount(
                        "HIGH",
                        -1
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceProfitCurrencyPotential(
                        "US",
                        BigDecimal.ONE
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceProfitCurrencyPotential(
                        "USD",
                        new BigDecimal("-1.00")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceProfitOpportunitySummary(
                        -1,
                        0,
                        0,
                        0,
                        0,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }
}