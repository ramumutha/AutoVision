package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceProfitOpportunityRepositoryTests {

    @Autowired
    private ServiceProfitOpportunityRepository repository;

    @AfterEach
    void clearOpportunities() {
        repository.deleteAll();
    }

    @Test
    void persistsAndReloadsOpportunityByTenant() {

        UUID tenantId = UUID.randomUUID();

        ServiceProfitOpportunity opportunity =
                newOpportunity(
                        tenantId,
                        "DECLINED:RO-1001:10",
                        ServiceProfitPriority.HIGH
                );

        repository.saveAndFlush(opportunity);

        ServiceProfitOpportunity persisted =
                repository.findByIdAndTenantId(
                        opportunity.getId(),
                        tenantId
                ).orElseThrow();

        assertEquals(tenantId, persisted.getTenantId());
        assertEquals(
                "DECLINED:RO-1001:10",
                persisted.getOpportunityKey()
        );
        assertEquals(
                ServiceProfitOpportunityStatus.DETECTED,
                persisted.getStatus()
        );
        assertEquals(
                ServiceProfitOpportunityType.DECLINED_WORK,
                persisted.getOpportunityType()
        );
        assertEquals(
                ServiceProfitPriority.HIGH,
                persisted.getPriority()
        );

        assertEquals(
                0,
                new BigDecimal("250.0000")
                        .compareTo(persisted.getPotentialAmount())
        );

        assertEquals("USD", persisted.getCurrencyCode());
        assertNull(persisted.getCustomerId());
        assertNull(persisted.getVehicleId());
    }

    @Test
    void tenantScopedLookupDoesNotCrossTenantBoundary() {

        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        ServiceProfitOpportunity opportunity =
                newOpportunity(
                        tenantId,
                        "DECLINED:TENANT-1",
                        ServiceProfitPriority.HIGH
                );

        repository.saveAndFlush(opportunity);

        assertTrue(
                repository.findByIdAndTenantId(
                        opportunity.getId(),
                        tenantId
                ).isPresent()
        );

        assertFalse(
                repository.findByIdAndTenantId(
                        opportunity.getId(),
                        otherTenantId
                ).isPresent()
        );
    }

    @Test
    void opportunityKeyIsUniqueWithinTenant() {

        UUID tenantId = UUID.randomUUID();

        repository.saveAndFlush(
                newOpportunity(
                        tenantId,
                        "DECLINED:UNIQUE-1",
                        ServiceProfitPriority.HIGH
                )
        );

        ServiceProfitOpportunity duplicate =
                newOpportunity(
                        tenantId,
                        "DECLINED:UNIQUE-1",
                        ServiceProfitPriority.MEDIUM
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(duplicate)
        );
    }

    @Test
    void sameOpportunityKeyIsAllowedAcrossTenants() {

        UUID firstTenantId = UUID.randomUUID();
        UUID secondTenantId = UUID.randomUUID();

        repository.saveAndFlush(
                newOpportunity(
                        firstTenantId,
                        "DECLINED:SHARED-1",
                        ServiceProfitPriority.HIGH
                )
        );

        repository.saveAndFlush(
                newOpportunity(
                        secondTenantId,
                        "DECLINED:SHARED-1",
                        ServiceProfitPriority.MEDIUM
                )
        );

        assertTrue(
                repository.existsByTenantIdAndOpportunityKey(
                        firstTenantId,
                        "DECLINED:SHARED-1"
                )
        );

        assertTrue(
                repository.existsByTenantIdAndOpportunityKey(
                        secondTenantId,
                        "DECLINED:SHARED-1"
                )
        );

        assertEquals(
                1,
                repository.findAllByTenantId(firstTenantId).size()
        );

        assertEquals(
                1,
                repository.findAllByTenantId(secondTenantId).size()
        );
    }

    @Test
    void filtersByTenantStatusAndPriority() {

        UUID tenantId = UUID.randomUUID();

        repository.saveAndFlush(
                newOpportunity(
                        tenantId,
                        "DECLINED:HIGH-1",
                        ServiceProfitPriority.HIGH
                )
        );

        repository.saveAndFlush(
                newOpportunity(
                        tenantId,
                        "DECLINED:MEDIUM-1",
                        ServiceProfitPriority.MEDIUM
                )
        );

        repository.saveAndFlush(
                newOpportunity(
                        UUID.randomUUID(),
                        "DECLINED:OTHER-TENANT",
                        ServiceProfitPriority.HIGH
                )
        );

        List<ServiceProfitOpportunity> matches =
                repository.findAllByTenantIdAndPriorityAndStatus(
                        tenantId,
                        ServiceProfitPriority.HIGH,
                        ServiceProfitOpportunityStatus.DETECTED
                );

        assertEquals(1, matches.size());
        assertEquals(
                "DECLINED:HIGH-1",
                matches.getFirst().getOpportunityKey()
        );
    }

    private ServiceProfitOpportunity newOpportunity(
            UUID tenantId,
            String opportunityKey,
            ServiceProfitPriority priority
    ) {
        return ServiceProfitOpportunity.detect(
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                null,
                null,
                null,
                opportunityKey,
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                priority,
                ServiceProfitActionability.READY,
                "Declined service work",
                "Dealer source identifies previously declined work.",
                new BigDecimal("250.0000"),
                "usd",
                "DEALER_IMPORT",
                "RO_LINE",
                UUID.randomUUID().toString(),
                null,
                null,
                null,
                null,
                "R1-POLICY-1",
                null,
                OffsetDateTime.now()
        );
    }
}
