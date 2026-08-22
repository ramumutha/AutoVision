package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceProfitOpportunityQueryRepositoryTests {

    @Autowired
    private ServiceProfitOpportunityRepository opportunityRepository;

    @Autowired
    private ServiceProfitOpportunityQueryRepository queryRepository;

    @AfterEach
    void clearOpportunities() {
        opportunityRepository.deleteAll();
    }

    @Test
    void tenantGrantReturnsOnlyAuthenticatedTenant() {

        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        persist(
                tenantId,
                null,
                null,
                null,
                "OPP-TENANT-1",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.parse(
                        "2026-08-22T10:00:00+00:00"
                )
        );

        persist(
                otherTenantId,
                null,
                null,
                null,
                "OPP-OTHER-TENANT",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.parse(
                        "2026-08-22T11:00:00+00:00"
                )
        );

        ServiceProfitOpportunityPage page =
                queryRepository.findAuthorizedPage(
                        tenantId,
                        List.of(
                                new AuthorizationGrant(
                                        AuthorizationScopeType.TENANT,
                                        tenantId
                                )
                        ),
                        query(0, 25)
                );

        assertEquals(1, page.totalElements());
        assertEquals(
                "OPP-TENANT-1",
                page.items().getFirst().opportunityKey()
        );
    }

    @Test
    void dealerGrantReturnsOnlyContainedDealerOpportunities() {

        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID otherDealerId = UUID.randomUUID();

        persist(
                tenantId,
                dealerId,
                null,
                null,
                "OPP-DEALER-1",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.now()
        );

        persist(
                tenantId,
                otherDealerId,
                null,
                null,
                "OPP-DEALER-2",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.now()
        );

        ServiceProfitOpportunityPage page =
                queryRepository.findAuthorizedPage(
                        tenantId,
                        List.of(
                                new AuthorizationGrant(
                                        AuthorizationScopeType.DEALER,
                                        dealerId
                                )
                        ),
                        query(0, 25)
                );

        assertEquals(1, page.totalElements());
        assertEquals(
                "OPP-DEALER-1",
                page.items().getFirst().opportunityKey()
        );
    }

    @Test
    void branchGrantReturnsOnlyContainedBranchOpportunities() {

        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        persist(
                tenantId,
                dealerId,
                branchId,
                null,
                "OPP-BRANCH-1",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.now()
        );

        persist(
                tenantId,
                dealerId,
                UUID.randomUUID(),
                null,
                "OPP-BRANCH-2",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.now()
        );

        ServiceProfitOpportunityPage page =
                queryRepository.findAuthorizedPage(
                        tenantId,
                        List.of(
                                new AuthorizationGrant(
                                        AuthorizationScopeType.BRANCH,
                                        branchId
                                )
                        ),
                        query(0, 25)
                );

        assertEquals(1, page.totalElements());
        assertEquals(
                "OPP-BRANCH-1",
                page.items().getFirst().opportunityKey()
        );
    }

    @Test
    void locationGrantReturnsOnlyContainedLocationOpportunities() {

        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        persist(
                tenantId,
                dealerId,
                branchId,
                locationId,
                "OPP-LOCATION-1",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.now()
        );

        persist(
                tenantId,
                dealerId,
                branchId,
                UUID.randomUUID(),
                "OPP-LOCATION-2",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.now()
        );

        ServiceProfitOpportunityPage page =
                queryRepository.findAuthorizedPage(
                        tenantId,
                        List.of(
                                new AuthorizationGrant(
                                        AuthorizationScopeType.LOCATION,
                                        locationId
                                )
                        ),
                        query(0, 25)
                );

        assertEquals(1, page.totalElements());
        assertEquals(
                "OPP-LOCATION-1",
                page.items().getFirst().opportunityKey()
        );
    }

    @Test
    void appliesPriorityFilter() {

        UUID tenantId = UUID.randomUUID();

        persist(
                tenantId,
                null,
                null,
                null,
                "OPP-HIGH",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.now()
        );

        persist(
                tenantId,
                null,
                null,
                null,
                "OPP-MEDIUM",
                ServiceProfitPriority.MEDIUM,
                OffsetDateTime.now()
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

        ServiceProfitOpportunityPage page =
                queryRepository.findAuthorizedPage(
                        tenantId,
                        tenantGrant(tenantId),
                        query
                );

        assertEquals(1, page.totalElements());
        assertEquals(
                "OPP-HIGH",
                page.items().getFirst().opportunityKey()
        );
    }

    @Test
    void pagesWithDeterministicDetectedDescendingOrder() {

        UUID tenantId = UUID.randomUUID();

        persist(
                tenantId,
                null,
                null,
                null,
                "OPP-OLD",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.parse(
                        "2026-08-20T10:00:00+00:00"
                )
        );

        persist(
                tenantId,
                null,
                null,
                null,
                "OPP-MIDDLE",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.parse(
                        "2026-08-21T10:00:00+00:00"
                )
        );

        persist(
                tenantId,
                null,
                null,
                null,
                "OPP-NEW",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.parse(
                        "2026-08-22T10:00:00+00:00"
                )
        );

        ServiceProfitOpportunityPage first =
                queryRepository.findAuthorizedPage(
                        tenantId,
                        tenantGrant(tenantId),
                        query(0, 2)
                );

        ServiceProfitOpportunityPage second =
                queryRepository.findAuthorizedPage(
                        tenantId,
                        tenantGrant(tenantId),
                        query(1, 2)
                );

        assertEquals(3, first.totalElements());
        assertEquals(2, first.totalPages());

        assertEquals(
                List.of(
                        "OPP-NEW",
                        "OPP-MIDDLE"
                ),
                first.items()
                        .stream()
                        .map(
                                ServiceProfitOpportunityQueueItem
                                        ::opportunityKey
                        )
                        .toList()
        );

        assertEquals(
                List.of("OPP-OLD"),
                second.items()
                        .stream()
                        .map(
                                ServiceProfitOpportunityQueueItem
                                        ::opportunityKey
                        )
                        .toList()
        );
    }

    @Test
    void emptyGrantCollectionReturnsNoRows() {

        UUID tenantId = UUID.randomUUID();

        persist(
                tenantId,
                null,
                null,
                null,
                "OPP-HIDDEN",
                ServiceProfitPriority.HIGH,
                OffsetDateTime.now()
        );

        ServiceProfitOpportunityPage page =
                queryRepository.findAuthorizedPage(
                        tenantId,
                        List.of(),
                        query(0, 25)
                );

        assertTrue(page.items().isEmpty());
        assertEquals(0, page.totalElements());
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

    private ServiceProfitOpportunityQuery query(
            int page,
            int size
    ) {
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
                page,
                size,
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
            OffsetDateTime detectedAt
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
                        ServiceProfitActionability.READY,
                        key,
                        null,
                        null,
                        null,
                        "DEALER_IMPORT",
                        "RO_LINE",
                        key,
                        null,
                        null,
                        null,
                        null,
                        "R1-POLICY-1",
                        null,
                        detectedAt
                );

        opportunityRepository.saveAndFlush(
                opportunity
        );
    }
}
