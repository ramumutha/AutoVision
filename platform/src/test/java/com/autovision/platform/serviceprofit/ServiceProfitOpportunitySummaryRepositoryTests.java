package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceProfitOpportunitySummaryRepositoryTests {

    @Autowired
    private ServiceProfitOpportunityRepository opportunityRepository;

    @Autowired
    private ServiceProfitOpportunityQueryRepository queryRepository;

    @AfterEach
    void clearOpportunities() {
        opportunityRepository.deleteAll();
    }

    @Test
    void summarizesAuthorizedTenantWithoutCrossTenantLeakage() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        persist(
                tenantId,
                null,
                null,
                null,
                "TENANT-HIGH",
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                new BigDecimal("100.00"),
                "USD"
        );

        persist(
                tenantId,
                null,
                null,
                null,
                "TENANT-MEDIUM",
                ServiceProfitPriority.MEDIUM,
                ServiceProfitActionability.REVIEW_REQUIRED,
                new BigDecimal("200.00"),
                "USD"
        );

        persist(
                otherTenantId,
                null,
                null,
                null,
                "OTHER-TENANT",
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                new BigDecimal("900.00"),
                "USD"
        );

        ServiceProfitOpportunitySummary summary =
                queryRepository.findAuthorizedSummary(
                        tenantId,
                        tenantGrant(tenantId),
                        query()
                );

        assertEquals(2, summary.totalOpportunities());
        assertEquals(1, summary.highPriorityCount());
        assertEquals(1, summary.reviewRequiredCount());
        assertEquals(1, summary.readyCount());

        assertEquals(
                0,
                new BigDecimal("300.00").compareTo(
                        summary.potentialByCurrency()
                                .getFirst()
                                .amount()
                )
        );
    }

    @Test
    void dealerGrantRestrictsSummaryToAuthorizedDealer() {
        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        persist(
                tenantId,
                dealerId,
                null,
                null,
                "AUTHORIZED-DEALER",
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                new BigDecimal("150.00"),
                "USD"
        );

        persist(
                tenantId,
                UUID.randomUUID(),
                null,
                null,
                "OTHER-DEALER",
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                new BigDecimal("850.00"),
                "USD"
        );

        ServiceProfitOpportunitySummary summary =
                queryRepository.findAuthorizedSummary(
                        tenantId,
                        List.of(
                                new AuthorizationGrant(
                                        AuthorizationScopeType.DEALER,
                                        dealerId
                                )
                        ),
                        query()
                );

        assertEquals(1, summary.totalOpportunities());

        assertEquals(
                0,
                new BigDecimal("150.00").compareTo(
                        summary.potentialByCurrency()
                                .getFirst()
                                .amount()
                )
        );
    }

    @Test
    void branchAndLocationGrantsRestrictSummary() {
        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        persist(
                tenantId,
                dealerId,
                branchId,
                locationId,
                "AUTHORIZED-LOCATION",
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                new BigDecimal("125.00"),
                "USD"
        );

        persist(
                tenantId,
                dealerId,
                branchId,
                UUID.randomUUID(),
                "OTHER-LOCATION",
                ServiceProfitPriority.MEDIUM,
                ServiceProfitActionability.READY,
                new BigDecimal("225.00"),
                "USD"
        );

        persist(
                tenantId,
                dealerId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "OTHER-BRANCH",
                ServiceProfitPriority.MEDIUM,
                ServiceProfitActionability.READY,
                new BigDecimal("325.00"),
                "USD"
        );

        ServiceProfitOpportunitySummary branchSummary =
                queryRepository.findAuthorizedSummary(
                        tenantId,
                        List.of(
                                new AuthorizationGrant(
                                        AuthorizationScopeType.BRANCH,
                                        branchId
                                )
                        ),
                        query()
                );

        assertEquals(
                2,
                branchSummary.totalOpportunities()
        );

        ServiceProfitOpportunitySummary locationSummary =
                queryRepository.findAuthorizedSummary(
                        tenantId,
                        List.of(
                                new AuthorizationGrant(
                                        AuthorizationScopeType.LOCATION,
                                        locationId
                                )
                        ),
                        query()
                );

        assertEquals(
                1,
                locationSummary.totalOpportunities()
        );
    }

    @Test
    void businessFilterNarrowsAuthorizedSummary() {
        UUID tenantId = UUID.randomUUID();

        persist(
                tenantId,
                null,
                null,
                null,
                "FILTER-HIGH",
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                new BigDecimal("100.00"),
                "USD"
        );

        persist(
                tenantId,
                null,
                null,
                null,
                "FILTER-MEDIUM",
                ServiceProfitPriority.MEDIUM,
                ServiceProfitActionability.READY,
                new BigDecimal("200.00"),
                "USD"
        );

        ServiceProfitOpportunityQuery query =
                new ServiceProfitOpportunityQuery(
                        null,
                        ServiceProfitPriority.HIGH,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        25,
                        ServiceProfitOpportunitySort.DETECTED_DESC
                );

        ServiceProfitOpportunitySummary summary =
                queryRepository.findAuthorizedSummary(
                        tenantId,
                        tenantGrant(tenantId),
                        query
                );

        assertEquals(1, summary.totalOpportunities());
        assertEquals(1, summary.highPriorityCount());

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(
                        summary.potentialByCurrency()
                                .getFirst()
                                .amount()
                )
        );
    }

    @Test
    void keepsPotentialSeparatedByCurrency() {
        UUID tenantId = UUID.randomUUID();

        persist(
                tenantId,
                null,
                null,
                null,
                "USD-OPPORTUNITY",
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                new BigDecimal("100.00"),
                "USD"
        );

        persist(
                tenantId,
                null,
                null,
                null,
                "EUR-OPPORTUNITY",
                ServiceProfitPriority.MEDIUM,
                ServiceProfitActionability.READY,
                new BigDecimal("200.00"),
                "EUR"
        );

        ServiceProfitOpportunitySummary summary =
                queryRepository.findAuthorizedSummary(
                        tenantId,
                        tenantGrant(tenantId),
                        query()
                );

        assertEquals(
                2,
                summary.potentialByCurrency().size()
        );

        assertEquals(
                List.of("EUR", "USD"),
                summary.potentialByCurrency()
                        .stream()
                        .map(
                                ServiceProfitCurrencyPotential
                                        ::currencyCode
                        )
                        .toList()
        );
    }

    @Test
    void emptyAuthorizedResultProducesZeroSummary() {
        UUID tenantId = UUID.randomUUID();

        ServiceProfitOpportunitySummary summary =
                queryRepository.findAuthorizedSummary(
                        tenantId,
                        List.of(),
                        query()
                );

        assertEquals(0, summary.totalOpportunities());
        assertEquals(0, summary.highPriorityCount());
        assertEquals(0, summary.reviewRequiredCount());
        assertEquals(0, summary.readyCount());
        assertEquals(0, summary.suppressedCount());

        assertTrue(summary.potentialByCurrency().isEmpty());
        assertTrue(summary.byOpportunityType().isEmpty());
        assertTrue(summary.byPriority().isEmpty());
        assertTrue(summary.byActionability().isEmpty());
    }

    private List<AuthorizationGrant> tenantGrant(
            UUID tenantId
    ) {
        return List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        tenantId
                )
        );
    }

    private ServiceProfitOpportunityQuery query() {
        return new ServiceProfitOpportunityQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                25,
                ServiceProfitOpportunitySort.DETECTED_DESC
        );
    }

    private void persist(
            UUID tenantId,
            UUID dealerId,
            UUID branchId,
            UUID locationId,
            String key,
            ServiceProfitPriority priority,
            ServiceProfitActionability actionability,
            BigDecimal potentialAmount,
            String currencyCode
    ) {
        ServiceProfitOpportunity opportunity =
                ServiceProfitOpportunity.detect(
                        UUID.randomUUID(),
                        tenantId,
                        dealerId,
                        branchId,
                        locationId,
                        null,
                        null,
                        key,
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                        ServiceProfitEvidenceStrength.STRONG,
                        priority,
                        actionability,
                        key,
                        null,
                        potentialAmount,
                        currencyCode,
                        "DEALER_IMPORT",
                        "RO_LINE",
                        key,
                        null,
                        null,
                        null,
                        null,
                        "R1-POLICY-1",
                        null,
                        OffsetDateTime.now()
                );

        opportunityRepository.saveAndFlush(
                opportunity
        );
    }
}