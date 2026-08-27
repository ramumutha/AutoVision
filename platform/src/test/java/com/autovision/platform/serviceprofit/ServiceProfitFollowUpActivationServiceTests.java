package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitFollowUpActivationServiceTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-27T00:00:00Z");

    private final ServiceProfitFollowUpPersistenceService persistence = mock(ServiceProfitFollowUpPersistenceService.class);
    private final ServiceProfitFollowUpActivationService service =
            new ServiceProfitFollowUpActivationService(persistence);

    @Test
    void readyOpportunityCreatesOpenUnassignedSystemWorkItem() {
        ServiceProfitOpportunity opportunity = opportunity(ServiceProfitActionability.READY);
        ServiceProfitFollowUp followUp = ServiceProfitFollowUp.open(
                UUID.randomUUID(), opportunity.getTenantId(), opportunity.getId(), NOW);
        when(persistence.ensureSystem(opportunity.getTenantId(), opportunity.getId(), NOW))
                .thenReturn(new ServiceProfitFollowUpPersistenceResult(followUp, true));

        ServiceProfitFollowUpActivationResult result = service.activate(opportunity, NOW);

        assertEquals(opportunity.getId(), result.opportunityId());
        assertEquals(followUp.getId(), result.followUpId());
        assertEquals(true, result.created());
        assertEquals(ServiceProfitFollowUpHandlingStatus.OPEN, result.handlingStatus());
        assertNull(result.ownerPrincipalId());
        assertEquals(ServiceProfitFollowUpDisposition.NONE, result.disposition());
        assertNull(result.nextActionDueAt());
    }

    @Test
    void replayReusesFollowUpAndReturnsItsExistingOperationalState() {
        ServiceProfitOpportunity opportunity = opportunity(ServiceProfitActionability.READY);
        ServiceProfitFollowUp followUp = ServiceProfitFollowUp.open(
                UUID.randomUUID(), opportunity.getTenantId(), opportunity.getId(), NOW);
        followUp.claim(UUID.randomUUID(), NOW);
        when(persistence.ensureSystem(any(), any(), any()))
                .thenReturn(new ServiceProfitFollowUpPersistenceResult(followUp, false));

        ServiceProfitFollowUpActivationResult result = service.activate(opportunity, NOW);

        assertEquals(false, result.created());
        assertEquals(followUp.getOwnerPrincipalId(), result.ownerPrincipalId());
        verify(persistence).ensureSystem(opportunity.getTenantId(), opportunity.getId(), NOW);
    }

    @Test
    void reviewRequiredAndSuppressedOpportunitiesDoNotActivate() {
        assertNull(service.activate(opportunity(ServiceProfitActionability.REVIEW_REQUIRED), NOW));
        assertNull(service.activate(opportunity(ServiceProfitActionability.SUPPRESSED), NOW));
        verify(persistence, never()).ensureSystem(any(), any(), any());
    }

    private ServiceProfitOpportunity opportunity(ServiceProfitActionability actionability) {
        return ServiceProfitOpportunity.detect(
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null,
                "OPP-ACTIVATION-1", ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED, ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH, actionability, "Declined work", null, null, null,
                "DMS", "SERVICE_LINE", "line-1", null, null, null, null,
                "R1-TEST", UUID.randomUUID(), NOW);
    }
}