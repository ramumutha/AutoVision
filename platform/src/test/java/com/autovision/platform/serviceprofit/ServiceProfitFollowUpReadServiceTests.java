package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitFollowUpReadServiceTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-27T10:00:00Z");
    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final UUID opportunityId = UUID.randomUUID();
    private final AuthenticatedTenantContext context = new AuthenticatedTenantContext(principalId, tenantId, "advisor");
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ServiceProfitOpportunityRepository opportunities = mock(ServiceProfitOpportunityRepository.class);
    private final ServiceProfitFollowUpRepository followUps = mock(ServiceProfitFollowUpRepository.class);
    private final ServiceProfitFollowUpHistoryRepository history = mock(ServiceProfitFollowUpHistoryRepository.class);
    private final ServiceProfitFollowUpReadService service = new ServiceProfitFollowUpReadService(
            authorization, opportunities, followUps, history);

    @Test
    void readsTenantQualifiedFollowUpWithSafeOwnershipAndDueState() {
        ServiceProfitFollowUp followUp = ServiceProfitFollowUp.open(UUID.randomUUID(), tenantId, opportunityId, NOW.minusDays(1));
        followUp.claim(principalId, NOW.minusHours(1));
        followUp.changeDueAt(NOW.minusMinutes(1), NOW);
        when(opportunities.findByIdAndTenantId(opportunityId, tenantId)).thenReturn(Optional.of(mock(ServiceProfitOpportunity.class)));
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));

        ServiceProfitFollowUpResponse response = service.readCurrent(context, opportunityId);

        assertEquals(ServiceProfitFollowUpOwnership.MINE, response.ownership());
        assertEquals(ServiceProfitWorkQueueDueState.OVERDUE, response.dueState());
        assertFalse(response.toString().contains(principalId.toString()));
        verify(authorization).requirePermission(org.mockito.ArgumentMatchers.argThat(request ->
            request.permissionCode().equals(ServiceProfitPermissions.FOLLOW_UP_READ)));
        verify(opportunities).findByIdAndTenantId(opportunityId, tenantId);
    }

    @Test
    void readsHistoryInRepositoryOrderAndMapsHumanAndSystemIdentitySafely() {
        when(opportunities.findByIdAndTenantId(opportunityId, tenantId)).thenReturn(Optional.of(mock(ServiceProfitOpportunity.class)));
        ServiceProfitFollowUp followUp = ServiceProfitFollowUp.open(UUID.randomUUID(), tenantId, opportunityId, NOW);
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.of(followUp));
        ServiceProfitFollowUpHistory human = ServiceProfitFollowUpHistory.record(UUID.randomUUID(), tenantId, followUp.getId(), opportunityId,
                principalId, ServiceProfitFollowUpEventType.DISPOSITION_CHANGED, "NONE", "FOLLOW_UP_REQUIRED", 1, 2, NOW);
        ServiceProfitFollowUpHistory system = ServiceProfitFollowUpHistory.recordSystem(UUID.randomUUID(), tenantId, followUp.getId(), opportunityId,
                ServiceProfitFollowUpEventType.CREATED, null, "OPEN", 0, 0, NOW.minusDays(1));
        when(history.findAllByTenantIdAndFollowUpIdOrderByOccurredAtAscIdAsc(tenantId, followUp.getId()))
                .thenReturn(List.of(system, human));

        List<ServiceProfitFollowUpHistoryResponse> response = service.readHistory(context, opportunityId);

        assertEquals("AUTOVISION_SERVICE_PROFIT", response.get(0).actorIdentity());
        assertEquals("ME", response.get(1).actorIdentity());
        assertEquals("AUTOVISION_SERVICE_PROFIT", response.get(0).applicationIdentity());
        assertEquals(null, response.get(1).applicationIdentity());
        verify(history).findAllByTenantIdAndFollowUpIdOrderByOccurredAtAscIdAsc(tenantId, followUp.getId());
        verify(history, never()).save(any());
        verify(followUps, never()).save(any());
    }

    @Test
    void missingOpportunityAndMissingFollowUpAreNotFoundWithoutWrites() {
        when(opportunities.findByIdAndTenantId(opportunityId, tenantId)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> service.readCurrent(context, opportunityId));
        when(opportunities.findByIdAndTenantId(opportunityId, tenantId)).thenReturn(Optional.of(mock(ServiceProfitOpportunity.class)));
        when(followUps.findByTenantIdAndOpportunityId(tenantId, opportunityId)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> service.readHistory(context, opportunityId));
        verify(followUps, never()).save(any());
        verify(history, never()).save(any());
    }
}