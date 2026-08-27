package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import org.springframework.dao.DataIntegrityViolationException;

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
        void systemEnsureCreatesSystemHistoryWithoutChangingFollowUpDefaults() {
                UUID tenantId = UUID.randomUUID();
                UUID opportunityId = UUID.randomUUID();
                ServiceProfitOpportunity opportunity = opportunity(tenantId, opportunityId);
                ServiceProfitOpportunityRepository opportunities = mock(ServiceProfitOpportunityRepository.class);
                ServiceProfitFollowUpRepository followUps = mock(ServiceProfitFollowUpRepository.class);
                ServiceProfitFollowUpHistoryRepository history = mock(ServiceProfitFollowUpHistoryRepository.class);
                when(opportunities.findByIdAndTenantId(opportunityId, tenantId)).thenReturn(java.util.Optional.of(opportunity));
                when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(java.util.Optional.empty());
                when(followUps.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
                when(history.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                ServiceProfitFollowUpPersistenceResult result = service(opportunities, followUps, history)
                                .ensureSystem(tenantId, opportunityId, NOW);

                assertEquals(true, result.created());
                assertEquals(ServiceProfitFollowUpHandlingStatus.OPEN, result.followUp().getHandlingStatus());
                assertNull(result.followUp().getOwnerPrincipalId());
                assertEquals(ServiceProfitFollowUpDisposition.NONE, result.followUp().getCurrentDisposition());
                assertNull(result.followUp().getNextActionDueAt());
                var historyCaptor = org.mockito.ArgumentCaptor.forClass(ServiceProfitFollowUpHistory.class);
                verify(history).save(historyCaptor.capture());
                assertEquals(ServiceProfitFollowUpHistoryActorType.SYSTEM, historyCaptor.getValue().getActorType());
                assertNull(historyCaptor.getValue().getActorPrincipalId());
                assertEquals(ServiceProfitFollowUpAuditApplication.AUTOVISION_SERVICE_PROFIT,
                                historyCaptor.getValue().getApplicationId());
        }

    @Test
    void systemEnsureResolvesDuplicateCreationAfterInnerTransactionRollsBack() {
        UUID tenantId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        ServiceProfitOpportunity opportunity = opportunity(tenantId, opportunityId);
        ServiceProfitOpportunityRepository opportunities = mock(ServiceProfitOpportunityRepository.class);
        ServiceProfitFollowUpRepository followUps = mock(ServiceProfitFollowUpRepository.class);
        ServiceProfitFollowUpHistoryRepository history = mock(ServiceProfitFollowUpHistoryRepository.class);
        ServiceProfitSystemFollowUpCreationService creator = mock(ServiceProfitSystemFollowUpCreationService.class);
        ServiceProfitFollowUp existing = ServiceProfitFollowUp.open(UUID.randomUUID(), tenantId, opportunityId, NOW);
        when(opportunities.findByIdAndTenantId(opportunityId, tenantId)).thenReturn(java.util.Optional.of(opportunity));
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId))
                .thenReturn(java.util.Optional.empty(), java.util.Optional.of(existing));
        doThrow(new DataIntegrityViolationException("duplicate follow-up"))
                .when(creator).create(tenantId, opportunityId, NOW);

        ServiceProfitFollowUpPersistenceResult result = new ServiceProfitFollowUpPersistenceService(
                opportunities, followUps, history, creator).ensureSystem(tenantId, opportunityId, NOW);

        assertFalse(result.created());
        assertEquals(existing.getId(), result.followUp().getId());
        verify(history, never()).save(any(ServiceProfitFollowUpHistory.class));
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
        assertEquals(ServiceProfitFollowUpHistoryActorType.HUMAN, history.getActorType());
        assertEquals(null, history.getApplicationId());
        assertEquals(0, history.getObservedVersion());
        assertEquals(0, history.getWrittenVersion());
        assertEquals(NOW, history.getOccurredAt());
    }

    @Test
    void humanHistoryRequiresAPrincipalIdentity() {
        assertThrows(IllegalArgumentException.class, () -> ServiceProfitFollowUpHistory.record(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                ServiceProfitFollowUpEventType.CREATED, null, "OPEN", 0, 0, NOW));
    }

    @Test
    void systemHistoryUsesApplicationIdentityWithoutAFakePrincipal() {
        ServiceProfitFollowUpHistory history = ServiceProfitFollowUpHistory.recordSystem(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ServiceProfitFollowUpEventType.CREATED, null, "OPEN", 0, 0, NOW);

        assertEquals(ServiceProfitFollowUpHistoryActorType.SYSTEM, history.getActorType());
        assertEquals(null, history.getActorPrincipalId());
        assertEquals(ServiceProfitFollowUpAuditApplication.AUTOVISION_SERVICE_PROFIT,
                history.getApplicationId());
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

    @Test
    void approvedDispositionVocabularyIsCompleteAndRejectsArbitraryValues() {
        assertEquals(java.util.List.of(
                        "NONE",
                        "FOLLOW_UP_REQUIRED",
                        "INTEREST_RECORDED",
                        "DECLINED_RECORDED",
                        "NO_RESPONSE_RECORDED",
                        "NO_FURTHER_ACTION"
                ),
                java.util.Arrays.stream(ServiceProfitFollowUpDisposition.values())
                        .map(Enum::name)
                        .toList());
        assertThrows(IllegalArgumentException.class,
                () -> ServiceProfitFollowUpDisposition.valueOf("AUTHORIZED"));
    }

    @Test
    void approvedDispositionValuesCanBeRepresentedInAppendOnlyHistory() {
        for (ServiceProfitFollowUpDisposition disposition : ServiceProfitFollowUpDisposition.values()) {
            ServiceProfitFollowUpHistory history = ServiceProfitFollowUpHistory.record(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    ServiceProfitFollowUpEventType.DISPOSITION_CHANGED,
                    ServiceProfitFollowUpDisposition.NONE.name(), disposition.name(), 0, 1, NOW);

            assertEquals(disposition.name(), history.getNewValue());
        }
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