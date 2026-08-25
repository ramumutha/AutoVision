package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class ServiceProfitFollowUpPersistenceTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-25T00:00:00Z");

    @Test
    void createsOpenFollowUpAndAppendOnlyCreationHistory() {
        UUID tenantId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ServiceProfitOpportunity opportunity = opportunity(tenantId, opportunityId);
        ServiceProfitOpportunityRepository opportunities = mock(ServiceProfitOpportunityRepository.class);
        ServiceProfitFollowUpRepository followUps = mock(ServiceProfitFollowUpRepository.class);
        ServiceProfitFollowUpHistoryRepository history = mock(ServiceProfitFollowUpHistoryRepository.class);
        when(opportunities.findByIdAndTenantId(opportunityId, tenantId)).thenReturn(java.util.Optional.of(opportunity));
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(java.util.Optional.empty());
        when(followUps.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(history.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProfitFollowUp result = service(opportunities, followUps, history)
                .create(tenantId, opportunityId, actorId, NOW);

        assertEquals(tenantId, result.getTenantId());
        assertEquals(opportunityId, result.getOpportunityId());
        assertEquals(ServiceProfitFollowUpHandlingStatus.OPEN, result.getHandlingStatus());
        assertEquals(ServiceProfitFollowUpDisposition.NONE, result.getCurrentDisposition());
        verify(history).save(any(ServiceProfitFollowUpHistory.class));
    }

    @Test
    void rejectsCrossTenantOpportunityLinkage() {
        ServiceProfitOpportunityRepository opportunities = mock(ServiceProfitOpportunityRepository.class);
        ServiceProfitFollowUpRepository followUps = mock(ServiceProfitFollowUpRepository.class);
        ServiceProfitFollowUpHistoryRepository history = mock(ServiceProfitFollowUpHistoryRepository.class);
        when(opportunities.findByIdAndTenantId(any(), any())).thenReturn(java.util.Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service(opportunities, followUps, history)
                .create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), NOW));
    }

        @Test
        void retriesReturnExistingFollowUpWithoutWritingHistory() {
        UUID tenantId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        ServiceProfitOpportunityRepository opportunities = mock(ServiceProfitOpportunityRepository.class);
        ServiceProfitFollowUpRepository followUps = mock(ServiceProfitFollowUpRepository.class);
        ServiceProfitFollowUpHistoryRepository history = mock(ServiceProfitFollowUpHistoryRepository.class);
        when(opportunities.findByIdAndTenantId(opportunityId, tenantId)).thenReturn(
                java.util.Optional.of(opportunity(tenantId, opportunityId)));
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(
                java.util.Optional.of(ServiceProfitFollowUp.open(UUID.randomUUID(), tenantId, opportunityId, NOW)));

        ServiceProfitFollowUpPersistenceService persistence = service(opportunities, followUps, history);
        ServiceProfitFollowUp existing = persistence
                .create(tenantId, opportunityId, UUID.randomUUID(), NOW);
        ServiceProfitFollowUp retried = persistence
                .create(tenantId, opportunityId, UUID.randomUUID(), NOW);

        assertEquals(opportunityId, existing.getOpportunityId());
        assertEquals(existing.getId(), retried.getId());
        verify(history, never()).save(any(ServiceProfitFollowUpHistory.class));
        verify(followUps, never()).save(any(ServiceProfitFollowUp.class));
        verify(opportunities, times(2)).findByIdAndTenantId(opportunityId, tenantId);
    }

    @Test
    void historyRecordPreservesActorTenantAndVersionsAndHasNoNoteField() {
        ServiceProfitFollowUpHistory history = ServiceProfitFollowUpHistory.record(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ServiceProfitFollowUpEventType.CREATED, null, "OPEN", 0, 0, NOW);

        assertFalse(java.util.Arrays.stream(ServiceProfitFollowUpHistory.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("internalNote")));
        assertEquals(ServiceProfitFollowUpEventType.CREATED, history.getEventType());
        assertEquals(0, history.getObservedVersion());
        assertEquals(0, history.getWrittenVersion());
        assertEquals(NOW, history.getOccurredAt());
    }

    @Test
    void boundedNoteLimitIsDefinedForFutureOperationalCommands() {
        assertEquals(500, ServiceProfitFollowUp.MAX_INTERNAL_NOTE_LENGTH);
    }

    @Test
    void ownerMayRemainNullBeforeClaimAndStatusVocabularyIsHandlingOnly() {
        ServiceProfitFollowUp followUp = ServiceProfitFollowUp.open(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), NOW);

        assertEquals(null, followUp.getOwnerPrincipalId());
        assertEquals(ServiceProfitFollowUpHandlingStatus.OPEN, followUp.getHandlingStatus());
        assertEquals(ServiceProfitFollowUpHandlingStatus.COMPLETED,
                ServiceProfitFollowUpHandlingStatus.valueOf("COMPLETED"));
        assertEquals(2, ServiceProfitFollowUpHandlingStatus.values().length);
    }

    @Test
    void followUpHasNoCommercialValueAndHistoryHasNoCommunicationBody() {
        assertFalse(java.util.Arrays.stream(ServiceProfitFollowUp.class.getDeclaredFields())
                .anyMatch(field -> field.getName().toLowerCase().contains("amount")
                        || field.getName().toLowerCase().contains("revenue")
                        || field.getName().toLowerCase().contains("value")));
        assertFalse(java.util.Arrays.stream(ServiceProfitFollowUpHistory.class.getDeclaredFields())
                .anyMatch(field -> field.getName().toLowerCase().contains("body")
                        || field.getName().toLowerCase().contains("content")
                        || field.getName().toLowerCase().contains("call")));
    }

    @Test
    void persistenceServiceHasNoCustomerAuthorizationOrServiceTransactionDependency() {
        assertFalse(java.util.Arrays.stream(ServiceProfitFollowUpPersistenceService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().getSimpleName().contains("Authorization")
                        || field.getType().getSimpleName().contains("ServiceOrder")
                        || field.getType().getSimpleName().contains("ServiceJob")
                        || field.getType().getSimpleName().contains("Invoice")));
    }

    @Test
    void historyExposesNoMutationMethodsAndSupportsControlledFutureEvents() {
        assertFalse(java.util.Arrays.stream(ServiceProfitFollowUpHistory.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().startsWith("set")));
        assertEquals(5, ServiceProfitFollowUpEventType.values().length);
        assertEquals(ServiceProfitFollowUpEventType.HANDLING_STATUS_CHANGED,
                ServiceProfitFollowUpEventType.valueOf("HANDLING_STATUS_CHANGED"));
    }

    private ServiceProfitFollowUpPersistenceService service(
            ServiceProfitOpportunityRepository opportunities,
            ServiceProfitFollowUpRepository followUps,
            ServiceProfitFollowUpHistoryRepository history
    ) {
        return new ServiceProfitFollowUpPersistenceService(opportunities, followUps, history);
    }

    private ServiceProfitOpportunity opportunity(UUID tenantId, UUID opportunityId) {
        return ServiceProfitOpportunity.detect(
                opportunityId, tenantId, null, null, null, null, null,
                "OPP-1", ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED, ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH, ServiceProfitActionability.READY,
                "Declined work", null, null, null, "DMS", "SERVICE_LINE", "line-1",
                null, null, null, null, "R1-TEST", UUID.randomUUID(), NOW);
    }
}