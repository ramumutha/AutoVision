package com.autovision.platform.serviceprofit;

import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceRef;
import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceSourceType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitOpportunityEvidenceTests {

    @Test
    void capturesSafeSourceLineageWithoutPayloadOrPii() {
        UUID tenantId = UUID.randomUUID();
        ServiceProfitOpportunity opportunity = ServiceProfitOpportunity.detect(
                UUID.randomUUID(), tenantId, null, null, null, null, null,
                "SP:DECLINED_WORK:TEST-1", ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED, ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH, ServiceProfitActionability.READY, "Declined work", null,
                null, null, "DMS", "SERVICE_JOB", "job-1", null, null, null, null,
                "R1-TEST", null, OffsetDateTime.parse("2026-08-25T00:00:00Z"));

        ServiceProfitOpportunityEvidence evidence = ServiceProfitOpportunityEvidence.capture(
                UUID.randomUUID(), opportunity,
                new ServiceProfitEvidenceRef(ServiceProfitEvidenceSourceType.DISPOSITION,
                        "disposition-1", "order-1", OffsetDateTime.parse("2026-08-25T00:00:00Z")),
                "DMS", ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                OffsetDateTime.parse("2026-08-25T00:00:00Z"));

        assertEquals(tenantId, evidence.getTenantId());
        assertEquals(opportunity.getId(), evidence.getOpportunityId());
        assertEquals(ServiceProfitEvidenceSourceType.DISPOSITION, evidence.getEvidenceSourceType());
        assertEquals("DMS", evidence.getSourceSystem());
        assertEquals("disposition-1", evidence.getSourceRecordId());
        assertEquals("order-1", evidence.getSourceParentRecordId());
        assertEquals(ServiceProfitEvidenceClass.SOURCE_CONFIRMED, evidence.getEvidenceClassification());
        assertEquals(ServiceProfitEvidenceStrength.STRONG, evidence.getEvidenceStrength());
        assertFalse(java.util.Arrays.stream(ServiceProfitOpportunityEvidence.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("rawPayload")));
    }

    @Test
    void repeatedPersistenceDoesNotDuplicateTheSameSourceEvidence() {
        ServiceProfitOpportunity opportunity = opportunity();
        ServiceProfitOpportunityEvidenceRepository repository =
                Mockito.mock(ServiceProfitOpportunityEvidenceRepository.class);
        ServiceProfitOpportunityEvidenceService service =
                new ServiceProfitOpportunityEvidenceService(repository);
        ServiceProfitEvidenceRef reference = new ServiceProfitEvidenceRef(
                ServiceProfitEvidenceSourceType.DISPOSITION, "disposition-1", "order-1", NOW);

        when(repository.existsByTenantIdAndOpportunityIdAndEvidenceSourceTypeAndSourceSystemAndSourceRecordId(
                any(), any(), any(), any(), any())).thenReturn(false, true);

        service.persist(opportunity, java.util.List.of(reference), "DMS",
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG, NOW);
        service.persist(opportunity, java.util.List.of(reference), "DMS",
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG, NOW);

        verify(repository, times(1)).save(any(ServiceProfitOpportunityEvidence.class));
    }

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-25T00:00:00Z");

    private ServiceProfitOpportunity opportunity() {
        return ServiceProfitOpportunity.detect(
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null,
                "SP:DECLINED_WORK:TEST-2", ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED, ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH, ServiceProfitActionability.READY, "Declined work", null,
                null, null, "DMS", "SERVICE_JOB", "job-1", null, null, null, null,
                "R1-TEST", null, NOW);
    }
}