package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceProfitOpportunityContextRepositoryTests {

    @Autowired
    private ServiceProfitOpportunityContextRepository contextRepository;

    @Autowired
    private ServiceProfitOpportunityRepository opportunityRepository;

    @AfterEach
    void clearData() {
        contextRepository.deleteAll();
        opportunityRepository.deleteAll();
    }

    @Test
    void persistsAndRetrievesDealerActionableContextByTenant() {
        UUID tenantId = UUID.randomUUID();
        ServiceProfitOpportunity opportunity = opportunity(tenantId);
        opportunityRepository.saveAndFlush(opportunity);

        contextRepository.saveAndFlush(
                ServiceProfitOpportunityContext.capture(
                        UUID.randomUUID(),
                        tenantId,
                        opportunity,
                        snapshot(),
                        OffsetDateTime.parse("2026-08-23T10:00:00Z")
                )
        );

        ServiceProfitOpportunityContext persisted = contextRepository
                .findByOpportunityIdAndTenantId(
                        opportunity.getId(),
                        tenantId
                )
                .orElseThrow();

        assertEquals("Arjun Mehta", persisted.getCustomerDisplayName());
        assertEquals("KA01AV1001", persisted.getVehicleRegistration());
        assertEquals("RO-1001", persisted.getServiceOrderReference());
        assertTrue(persisted.getCustomerContactable());
    }

    @Test
    void tenantScopedLookupCannotResolveAnotherTenantContext() {
        UUID tenantId = UUID.randomUUID();
        ServiceProfitOpportunity opportunity = opportunity(tenantId);
        opportunityRepository.saveAndFlush(opportunity);
        contextRepository.saveAndFlush(
                ServiceProfitOpportunityContext.capture(
                        UUID.randomUUID(), tenantId, opportunity,
                        snapshot(), OffsetDateTime.now()
                )
        );

        assertTrue(contextRepository.findByOpportunityIdAndTenantId(
                opportunity.getId(), tenantId
        ).isPresent());
        assertFalse(contextRepository.findByOpportunityIdAndTenantId(
                opportunity.getId(), UUID.randomUUID()
        ).isPresent());
    }

    @Test
    void rejectsContextWithMismatchedOpportunityTenant() {
        ServiceProfitOpportunity opportunity = opportunity(UUID.randomUUID());

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceProfitOpportunityContext.capture(
                        UUID.randomUUID(), UUID.randomUUID(),
                        opportunity,
                        snapshot(), OffsetDateTime.now()
                )
        );
    }

    private ServiceProfitOpportunityContextSnapshot snapshot() {
        return new ServiceProfitOpportunityContextSnapshot(
                "Arjun Mehta", "CUST-001", "+919900000001",
                "arjun.mehta@example.demo", true,
                "KA01AV1001", "VINDEMO00000000001", "Demo Motors",
                "City Prime", 2022, "ICE", "RO-1001",
                LocalDate.parse("2026-07-01"),
                "Front brake pad replacement",
                "Customer declined front brake work today."
        );
    }

    private ServiceProfitOpportunity opportunity(UUID tenantId) {
        return ServiceProfitOpportunity.detect(
                UUID.randomUUID(), tenantId, null, null, null,
                null, null, "CONTEXT:" + UUID.randomUUID(),
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                "Declined work", null, new BigDecimal("100.00"),
                "INR", "DEMO", "SERVICE_JOB", "JOB-1",
                null, null, null, null, "R1", null,
                OffsetDateTime.now()
        );
    }
}