package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceProfitFollowUpRepositoryTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-25T00:00:00Z");

    @Autowired
    private ServiceProfitFollowUpRepository followUpRepository;

    @Autowired
    private ServiceProfitOpportunityRepository opportunityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearData() {
        followUpRepository.deleteAll();
        opportunityRepository.deleteAll();
    }

    @Test
    void persistsAndReloadsAllApprovedDispositionValuesByTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        ServiceProfitOpportunity opportunity = opportunity(tenantId, opportunityId);
        opportunityRepository.saveAndFlush(opportunity);

        for (ServiceProfitFollowUpDisposition disposition : ServiceProfitFollowUpDisposition.values()) {
            ServiceProfitFollowUp followUp = ServiceProfitFollowUp.open(
                    UUID.randomUUID(), tenantId, opportunityId, NOW);
            followUpRepository.saveAndFlush(followUp);

            jdbcTemplate.update(
                "UPDATE platform.service_profit_follow_ups "
                    + "SET current_disposition = ? WHERE id = ? AND tenant_id = ?",
                disposition.name(), followUp.getId(), tenantId);
            followUpRepository.flush();

            ServiceProfitFollowUp reloaded = followUpRepository
                    .findByIdAndTenantId(followUp.getId(), tenantId)
                    .orElseThrow();
            assertEquals(disposition, reloaded.getCurrentDisposition());
            assertTrue(followUpRepository
                    .findByTenantIdAndOpportunityId(tenantId, opportunityId)
                    .isPresent());

            followUpRepository.delete(reloaded);
            followUpRepository.flush();
        }

        assertFalse(followUpRepository
                .findByIdAndTenantId(UUID.randomUUID(), UUID.randomUUID())
                .isPresent());
    }

    private ServiceProfitOpportunity opportunity(UUID tenantId, UUID opportunityId) {
        return ServiceProfitOpportunity.detect(
                opportunityId, tenantId, null, null, null, null, null,
                "OPP-RELOAD-1", ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED, ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH, ServiceProfitActionability.READY,
                "Declined work", null, null, null, "DMS", "SERVICE_LINE", "line-1",
                null, null, null, null, "R1-TEST", UUID.randomUUID(), NOW);
    }
}
