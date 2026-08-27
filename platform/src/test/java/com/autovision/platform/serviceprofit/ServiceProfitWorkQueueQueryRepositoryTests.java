package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceProfitWorkQueueQueryRepositoryTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-27T10:00:00Z");

    @Autowired
    private ServiceProfitWorkQueueQueryRepository queryRepository;
    @Autowired
    private ServiceProfitOpportunityRepository opportunityRepository;
    @Autowired
    private ServiceProfitFollowUpRepository followUpRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearData() {
        followUpRepository.deleteAll();
        opportunityRepository.deleteAll();
    }

    @Test
    void defaultsToOpenAndExcludesCompletedWork() {
        UUID tenantId = UUID.randomUUID();
        ServiceProfitFollowUp open = create(tenantId, "OPEN", null, null, NOW.minusDays(2));
        ServiceProfitFollowUp completed = create(tenantId, "COMPLETED", null, null, NOW.minusDays(1));
        markCompleted(completed);

        ServiceProfitWorkQueuePage page = find(tenantId, query());

        assertEquals(1, page.totalElements());
        assertEquals(open.getId(), page.items().getFirst().followUpId());
        assertEquals(ServiceProfitFollowUpHandlingStatus.OPEN, page.items().getFirst().handlingStatus());
    }

    @Test
    void completedViewReturnsHistoricalWorkWhenRequested() {
        UUID tenantId = UUID.randomUUID();
        ServiceProfitFollowUp completed = create(tenantId, "COMPLETED-VIEW", null, null, NOW);
        markCompleted(completed);

        ServiceProfitWorkQueuePage page = find(tenantId, new ServiceProfitWorkQueueQuery(
                ServiceProfitFollowUpHandlingStatus.COMPLETED,
                ServiceProfitWorkQueueOwnership.ALL,
                ServiceProfitWorkQueueDueState.ALL,
                null,
                0,
                25));

        assertEquals(List.of(completed.getId()), ids(page));
    }

    @Test
    void supportsUnassignedMineAndTenantIsolation() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        ServiceProfitFollowUp unassigned = create(tenantId, "UNASSIGNED", null, null, NOW);
        ServiceProfitFollowUp mine = create(tenantId, "MINE", null, null, NOW.plusMinutes(1));
        assign(mine, principalId);
        ServiceProfitFollowUp otherOwner = create(tenantId, "OTHER-OWNER", null, null, NOW.plusMinutes(2));
        assign(otherOwner, UUID.randomUUID());
        create(otherTenantId, "OTHER-TENANT", null, null, NOW.plusMinutes(3));

        assertEquals(List.of(unassigned.getId()), find(tenantId,
                query(ServiceProfitWorkQueueOwnership.UNASSIGNED, ServiceProfitWorkQueueDueState.ALL, null, 0, 25)).items()
                .stream().map(ServiceProfitWorkQueueItem::followUpId).toList());
        assertEquals(List.of(mine.getId()), find(tenantId,
                query(ServiceProfitWorkQueueOwnership.MINE, ServiceProfitWorkQueueDueState.ALL, null, 0, 25), principalId).items()
                .stream().map(ServiceProfitWorkQueueItem::followUpId).toList());
        assertEquals(3, find(tenantId, query()).totalElements());
    }

    @Test
    void supportsOverdueUpcomingAndNoDueDate() {
        UUID tenantId = UUID.randomUUID();
        OffsetDateTime currentTime = OffsetDateTime.now();
        ServiceProfitFollowUp overdue = create(tenantId, "OVERDUE", currentTime.minusHours(1), null, NOW);
        ServiceProfitFollowUp upcoming = create(tenantId, "UPCOMING", currentTime.plusHours(1), null, NOW.plusMinutes(1));
        ServiceProfitFollowUp noDue = create(tenantId, "NO-DUE", null, null, NOW.plusMinutes(2));

        assertEquals(List.of(overdue.getId()), ids(find(tenantId,
                query(ServiceProfitWorkQueueOwnership.ALL, ServiceProfitWorkQueueDueState.OVERDUE, null, 0, 25))));
        assertEquals(List.of(upcoming.getId()), ids(find(tenantId,
                query(ServiceProfitWorkQueueOwnership.ALL, ServiceProfitWorkQueueDueState.UPCOMING, null, 0, 25))));
        assertEquals(List.of(noDue.getId()), ids(find(tenantId,
                query(ServiceProfitWorkQueueOwnership.ALL, ServiceProfitWorkQueueDueState.NO_DUE_DATE, null, 0, 25))));
    }

    @Test
    void ordersOverdueThenDueThenNoDueAndPaginatesDeterministically() {
        UUID tenantId = UUID.randomUUID();
        ServiceProfitFollowUp overdue = create(tenantId, "OVERDUE", NOW.minusHours(1), null, NOW);
        ServiceProfitFollowUp due = create(tenantId, "DUE", NOW.plusHours(1), null, NOW.plusMinutes(1));
        ServiceProfitFollowUp noDue = create(tenantId, "NO-DUE", null, null, NOW.plusMinutes(2));

        ServiceProfitWorkQueuePage page = find(tenantId, query(ServiceProfitWorkQueueOwnership.ALL,
                ServiceProfitWorkQueueDueState.ALL, null, 0, 2));
        assertEquals(List.of(overdue.getId(), due.getId()), ids(page));
        assertEquals(3, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(List.of(noDue.getId()), ids(find(tenantId, query(ServiceProfitWorkQueueOwnership.ALL,
                ServiceProfitWorkQueueDueState.ALL, null, 1, 2))));
    }

    @Test
    void filtersDispositionAndReturnsSafeImmutableOperationalProjection() {
        UUID tenantId = UUID.randomUUID();
        ServiceProfitFollowUp followUp = create(tenantId, "DISPOSITION", null, ServiceProfitFollowUpDisposition.NONE, NOW);
        jdbcTemplate.update("UPDATE platform.service_profit_follow_ups SET current_disposition = 'FOLLOW_UP_REQUIRED' WHERE id = ?", followUp.getId());

        ServiceProfitWorkQueueItem item = find(tenantId, query(ServiceProfitWorkQueueOwnership.ALL,
                ServiceProfitWorkQueueDueState.ALL, ServiceProfitFollowUpDisposition.FOLLOW_UP_REQUIRED, 0, 25))
                .items().getFirst();

        assertEquals(followUp.getId(), item.followUpId());
        assertEquals(ServiceProfitFollowUpDisposition.FOLLOW_UP_REQUIRED, item.disposition());
        assertNull(item.ownerPrincipalId());
        assertFalse(java.util.Arrays.stream(ServiceProfitWorkQueueItem.class.getDeclaredFields())
                .anyMatch(field -> field.getName().toLowerCase().contains("payload")
                        || field.getName().toLowerCase().contains("customer")
                        || field.getName().toLowerCase().contains("amount")));
        assertTrue(find(tenantId, query()).items().getFirst().title() != null);
    }

    @Test
    void queryDoesNotMutateFollowUpOrOpportunity() {
        UUID tenantId = UUID.randomUUID();
        ServiceProfitFollowUp followUp = create(tenantId, "IMMUTABLE", NOW.plusHours(1), null, NOW);
        long version = followUp.getVersion();
        OffsetDateTime updatedAt = followUp.getUpdatedAt();
        ServiceProfitWorkQueuePage page = find(tenantId, query());

        ServiceProfitFollowUp reloaded = followUpRepository.findByIdAndTenantId(followUp.getId(), tenantId).orElseThrow();
        assertEquals(1, page.totalElements());
        assertEquals(version, reloaded.getVersion());
        assertEquals(updatedAt, reloaded.getUpdatedAt());
    }

    private List<UUID> ids(ServiceProfitWorkQueuePage page) {
        return page.items().stream().map(ServiceProfitWorkQueueItem::followUpId).toList();
    }

    private ServiceProfitWorkQueuePage find(UUID tenantId, ServiceProfitWorkQueueQuery query) {
        return find(tenantId, query, UUID.randomUUID());
    }

    private ServiceProfitWorkQueuePage find(UUID tenantId, ServiceProfitWorkQueueQuery query, UUID principalId) {
        return queryRepository.findAuthorizedPage(tenantId, principalId,
                List.of(new AuthorizationGrant(AuthorizationScopeType.TENANT, tenantId)), query);
    }

    private ServiceProfitWorkQueueQuery query() {
        return query(ServiceProfitWorkQueueOwnership.ALL, ServiceProfitWorkQueueDueState.ALL, null, 0, 25);
    }

    private ServiceProfitWorkQueueQuery query(ServiceProfitWorkQueueOwnership ownership,
                                              ServiceProfitWorkQueueDueState dueState,
                                              ServiceProfitFollowUpDisposition disposition,
                                              int page, int size) {
        return new ServiceProfitWorkQueueQuery(ServiceProfitFollowUpHandlingStatus.OPEN, ownership, dueState,
                disposition, page, size);
    }

    private ServiceProfitFollowUp create(UUID tenantId, String key, OffsetDateTime dueAt,
                                         ServiceProfitFollowUpDisposition disposition, OffsetDateTime createdAt) {
        UUID opportunityId = UUID.randomUUID();
        ServiceProfitOpportunity opportunity = ServiceProfitOpportunity.detect(opportunityId, tenantId, null, null, null,
                null, null, "QUEUE-" + key, ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED, ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH, ServiceProfitActionability.READY, "Queue " + key, "Safe summary", null, null,
                "DMS", "SERVICE_LINE", "line-" + key, null, null, null, null, "R1-TEST", UUID.randomUUID(), createdAt);
        opportunityRepository.saveAndFlush(opportunity);
        ServiceProfitFollowUp followUp = ServiceProfitFollowUp.open(UUID.randomUUID(), tenantId, opportunityId, createdAt);
        followUpRepository.saveAndFlush(followUp);
        if (dueAt != null) jdbcTemplate.update("UPDATE platform.service_profit_follow_ups SET next_action_due_at = ? WHERE id = ?", dueAt, followUp.getId());
        if (disposition != null) jdbcTemplate.update("UPDATE platform.service_profit_follow_ups SET current_disposition = ? WHERE id = ?", disposition.name(), followUp.getId());
        return followUpRepository.findByIdAndTenantId(followUp.getId(), tenantId).orElseThrow();
    }

    private void assign(ServiceProfitFollowUp followUp, UUID principalId) {
        jdbcTemplate.update("UPDATE platform.service_profit_follow_ups SET owner_principal_id = ?, claimed_at = ? WHERE id = ?",
                principalId, NOW, followUp.getId());
    }

    private void markCompleted(ServiceProfitFollowUp followUp) {
        jdbcTemplate.update("UPDATE platform.service_profit_follow_ups SET handling_status = 'COMPLETED' WHERE id = ?", followUp.getId());
    }
}