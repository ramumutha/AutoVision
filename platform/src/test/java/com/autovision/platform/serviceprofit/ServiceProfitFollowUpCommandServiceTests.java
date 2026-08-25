package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitFollowUpCommandServiceTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-25T00:00:00Z");

    private final ServiceProfitOpportunityAccessService opportunityAccess = mock(ServiceProfitOpportunityAccessService.class);
    private final ServiceProfitFollowUpPersistenceService persistence = mock(ServiceProfitFollowUpPersistenceService.class);
    private final ServiceProfitFollowUpRepository followUps = mock(ServiceProfitFollowUpRepository.class);
    private final ServiceProfitFollowUpHistoryRepository history = mock(ServiceProfitFollowUpHistoryRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final UUID opportunityId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "operator");

    private ServiceProfitFollowUpCommandService service;

    @BeforeEach
    void setUp() {
        service = new ServiceProfitFollowUpCommandService(
                opportunityAccess, persistence, followUps, history, authorization);
        doNothing().when(authorization).requirePermission(any());
        when(opportunityAccess.requireOpportunity(any(AuthenticatedTenantContext.class), eq(opportunityId)))
                .thenReturn(opportunity(ServiceProfitActionability.READY));
        when(followUps.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(history.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void ensureCreatesEligibleFollowUpAndRetryReturnsExistingWithoutCreationHistory() {
        ServiceProfitFollowUp followUp = followUp();
        when(persistence.create(eq(tenantId), eq(opportunityId), eq(principalId), any(OffsetDateTime.class)))
            .thenReturn(followUp);

        assertEquals(followUp.getId(), service.ensure(context, opportunityId).followUpId());
        verify(persistence).create(eq(tenantId), eq(opportunityId), eq(principalId), any(OffsetDateTime.class));

        when(persistence.create(eq(tenantId), eq(opportunityId), eq(principalId), any(OffsetDateTime.class)))
            .thenReturn(followUp);
        assertEquals(followUp.getId(), service.ensure(context, opportunityId).followUpId());
        verify(history, never()).save(any());
    }

    @Test
    void reviewRequiredMayBeEnsuredAndClaimedButCannotBeHandledOperationally() {
        ServiceProfitOpportunity reviewRequired = opportunity(ServiceProfitActionability.REVIEW_REQUIRED);
        when(opportunityAccess.requireOpportunity(any(AuthenticatedTenantContext.class), eq(opportunityId)))
            .thenReturn(reviewRequired);
        ServiceProfitFollowUp followUp = followUp();
        when(persistence.create(any(), any(), any(), any())).thenReturn(followUp);
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));

        assertEquals(followUp.getId(), service.ensure(context, opportunityId).followUpId());
        assertEquals(followUp.getId(), service.selfClaim(context, opportunityId, 0).followUpId());
        assertThrows(ResponseStatusException.class,
                () -> service.setNextActionDueAt(context, opportunityId, NOW.plusDays(1), 0));
    }

    @Test
    void selfClaimDerivesOwnerAndSameOwnerRetryDoesNotWriteAnotherEvent() {
        ServiceProfitFollowUp followUp = followUp();
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));

        service.selfClaim(context, opportunityId, 0);
        assertEquals(principalId, followUp.getOwnerPrincipalId());
        verify(history).save(any(ServiceProfitFollowUpHistory.class));

        service.selfClaim(context, opportunityId, 0);
        verify(history).save(any(ServiceProfitFollowUpHistory.class));
    }

    @Test
    void differentPrincipalCannotOverwriteClaim() {
        ServiceProfitFollowUp followUp = followUp();
        UUID otherPrincipal = UUID.randomUUID();
        followUp.claim(otherPrincipal, NOW);
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));
        AuthenticatedTenantContext otherContext = new AuthenticatedTenantContext(principalId, tenantId, "other");

        assertThrows(ResponseStatusException.class,
                () -> service.selfClaim(otherContext, opportunityId, 0));
        verify(followUps, never()).saveAndFlush(any());
    }

    @Test
    void dueDateChangeRecordsStateAndIdenticalRetryIsNoOp() {
        ServiceProfitFollowUp followUp = ownedFollowUp();
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));
        OffsetDateTime dueAt = NOW.plusDays(2);

        service.setNextActionDueAt(context, opportunityId, dueAt, 0);
        assertEquals(dueAt, followUp.getNextActionDueAt());
        verify(history).save(any(ServiceProfitFollowUpHistory.class));

        service.setNextActionDueAt(context, opportunityId, dueAt, 0);
        verify(history).save(any(ServiceProfitFollowUpHistory.class));
    }

    @Test
    void dispositionChangeRecordsControlledValueAndIdenticalRetryIsNoOp() {
        ServiceProfitFollowUp followUp = ownedFollowUp();
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));

        service.recordDisposition(context, opportunityId,
                ServiceProfitFollowUpDisposition.INTEREST_RECORDED, 0);
        assertEquals(ServiceProfitFollowUpDisposition.INTEREST_RECORDED, followUp.getCurrentDisposition());
        verify(history).save(any(ServiceProfitFollowUpHistory.class));

        service.recordDisposition(context, opportunityId,
                ServiceProfitFollowUpDisposition.INTEREST_RECORDED, 0);
        verify(history).save(any(ServiceProfitFollowUpHistory.class));
    }

    @Test
    void completionIsInternalAndRepeatCompletionIsNoOp() {
        ServiceProfitFollowUp followUp = ownedFollowUp();
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));

        ServiceProfitFollowUpResult result = service.completeHandling(context, opportunityId, 0);
        assertEquals(ServiceProfitFollowUpHandlingStatus.COMPLETED, result.handlingStatus());
        verify(history).save(any(ServiceProfitFollowUpHistory.class));

        service.completeHandling(context, opportunityId, 0);
        verify(history).save(any(ServiceProfitFollowUpHistory.class));
    }

    @Test
    void staleVersionIsRejectedBeforeAnyMutation() {
        ServiceProfitFollowUp followUp = ownedFollowUp();
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));

        assertThrows(ResponseStatusException.class,
                () -> service.setNextActionDueAt(context, opportunityId, NOW.plusDays(1), 1));
        verify(followUps, never()).saveAndFlush(any());
        verify(history, never()).save(any());
    }

    @Test
    void readCurrentAndHistoryUseTenantQualifiedRepositories() {
        ServiceProfitFollowUp followUp = followUp();
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));
        when(history.findAllByTenantIdAndFollowUpIdOrderByOccurredAtAscIdAsc(tenantId, followUp.getId()))
                .thenReturn(List.of());

        assertEquals(followUp.getId(), service.readCurrent(context, opportunityId).followUpId());
        assertEquals(List.of(), service.readHistory(context, opportunityId));
        verify(followUps, org.mockito.Mockito.times(2))
            .findByTenantIdAndOpportunityId(tenantId, opportunityId);
        verify(history).findAllByTenantIdAndFollowUpIdOrderByOccurredAtAscIdAsc(tenantId, followUp.getId());
    }

    @Test
    void nonActionableOpportunityIsRejectedBeforeFollowUpCreation() {
        when(opportunityAccess.requireOpportunity(context, opportunityId))
                .thenReturn(opportunity(ServiceProfitActionability.SUPPRESSED));

        assertThrows(ResponseStatusException.class, () -> service.ensure(context, opportunityId));
        verify(persistence, never()).create(any(), any(), any(), any());
    }

    private ServiceProfitFollowUp followUp() {
        return ServiceProfitFollowUp.open(UUID.randomUUID(), tenantId, opportunityId, NOW);
    }

    private ServiceProfitFollowUp ownedFollowUp() {
        ServiceProfitFollowUp followUp = followUp();
        followUp.claim(principalId, NOW);
        return followUp;
    }

    private ServiceProfitOpportunity opportunity(ServiceProfitActionability actionability) {
        return ServiceProfitOpportunity.detect(
                opportunityId, tenantId, null, null, null, null, null,
                "OPP-COMMAND-1", ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED, ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH, actionability, "Declined work", null,
                null, null, "DMS", "SERVICE_LINE", "line-1", null, null, null, null,
                "R1-TEST", UUID.randomUUID(), NOW);
    }

}