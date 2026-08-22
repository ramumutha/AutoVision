package com.autovision.platform.serviceprofit;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceProfitOpportunitySuppressionRepositoryTests {

    @Autowired
    private ServiceProfitOpportunityRepository repository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearOpportunities() {
        repository.deleteAll();
    }

    @Test
    void suppressionStatePersistsAndReloads() {

        UUID tenantId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        ServiceProfitOpportunity opportunity =
                newOpportunity(
                        tenantId,
                        "DECLINED:SUPPRESSION:001"
                );

        repository.saveAndFlush(opportunity);

        OffsetDateTime suppressedAt =
                OffsetDateTime.parse(
                        "2026-08-22T10:30:00+05:30"
                );

        opportunity.suppress(
                ServiceProfitSuppressionReason.WORK_ALREADY_COMPLETED,
                principalId,
                suppressedAt
        );

        repository.saveAndFlush(opportunity);

        entityManager.clear();

        ServiceProfitOpportunity persisted =
                repository.findByIdAndTenantId(
                        opportunity.getId(),
                        tenantId
                ).orElseThrow();

        assertEquals(
                ServiceProfitOpportunityStatus.SUPPRESSED,
                persisted.getStatus()
        );

        assertEquals(
                ServiceProfitActionability.SUPPRESSED,
                persisted.getActionability()
        );

        assertEquals(
                ServiceProfitSuppressionReason.WORK_ALREADY_COMPLETED,
                persisted.getSuppressionReason()
        );

        assertEquals(
                suppressedAt,
                persisted.getSuppressedAt()
        );

        assertEquals(
                principalId,
                persisted.getUpdatedByPrincipalId()
        );
    }

    @Test
    void suppressionUpdateAdvancesOptimisticVersion() {

        UUID tenantId = UUID.randomUUID();

        ServiceProfitOpportunity opportunity =
                newOpportunity(
                        tenantId,
                        "DECLINED:SUPPRESSION:VERSION"
                );

        ServiceProfitOpportunity persisted =
                repository.saveAndFlush(opportunity);

        long initialVersion =
                persisted.getVersion();

        persisted.suppress(
                ServiceProfitSuppressionReason.AUTHORITATIVE_COMPLETION_EVIDENCE,
                UUID.randomUUID(),
                OffsetDateTime.parse(
                        "2026-08-22T11:00:00+05:30"
                )
        );

        ServiceProfitOpportunity updated =
                repository.saveAndFlush(persisted);

        long updatedVersion =
                updated.getVersion();

        assertTrue(
                updatedVersion > initialVersion,
                "Suppressing an opportunity must advance its optimistic version"
        );

        entityManager.clear();

        ServiceProfitOpportunity reloaded =
                repository.findByIdAndTenantId(
                        updated.getId(),
                        tenantId
                ).orElseThrow();

        assertEquals(
                updatedVersion,
                reloaded.getVersion()
        );

        assertNotNull(
                reloaded.getSuppressedAt()
        );
    }

    private ServiceProfitOpportunity newOpportunity(
            UUID tenantId,
            String opportunityKey
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
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                "Declined service work",
                "Dealer source identifies previously declined work.",
                new BigDecimal("250.0000"),
                "USD",
                "DEALER_IMPORT",
                "RO_LINE",
                UUID.randomUUID().toString(),
                null,
                null,
                null,
                null,
                "R1-POLICY-1",
                null,
                OffsetDateTime.parse(
                        "2026-08-22T09:00:00+05:30"
                )
        );
    }
}