package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationGrant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ServiceProfitWorkQueueQueryRepository {
    private final JdbcClient jdbcClient;

    public ServiceProfitWorkQueueQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ServiceProfitWorkQueuePage findAuthorizedPage(
            UUID authenticatedTenantId,
            UUID authenticatedPrincipalId,
            List<AuthorizationGrant> grants,
            ServiceProfitWorkQueueQuery query
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("authenticatedTenantId", authenticatedTenantId);
        String predicate = buildPredicate(authenticatedTenantId, authenticatedPrincipalId, grants, query, params);
        String from = """
                FROM platform.service_profit_follow_ups sfpf
                JOIN platform.service_profit_opportunities spo
                  ON spo.id = sfpf.opportunity_id
                 AND spo.tenant_id = sfpf.tenant_id
                WHERE
                """ + predicate + System.lineSeparator();

        long total = jdbcClient.sql("SELECT COUNT(*) " + from).params(params).query(Long.class).single();
        if (total == 0) return ServiceProfitWorkQueuePage.of(List.of(), query.page(), query.size(), 0);

        Map<String, Object> pageParams = new HashMap<>(params);
        pageParams.put("limit", query.size());
        pageParams.put("offset", query.offset());
        List<ServiceProfitWorkQueueItem> items = jdbcClient.sql("""
                SELECT sfpf.id AS follow_up_id, sfpf.opportunity_id, sfpf.handling_status,
                       sfpf.owner_principal_id, sfpf.claimed_at, sfpf.next_action_due_at,
                       sfpf.current_disposition, sfpf.version AS follow_up_version,
                       sfpf.created_at AS follow_up_created_at, sfpf.updated_at AS follow_up_updated_at,
                       spo.actionability, spo.status AS opportunity_status, spo.evidence_class,
                       spo.evidence_strength, spo.priority, spo.title, spo.summary
                """ + from + """
                ORDER BY CASE
                    WHEN sfpf.handling_status = 'OPEN' AND sfpf.next_action_due_at < CURRENT_TIMESTAMP THEN 0
                    WHEN sfpf.handling_status = 'OPEN' AND sfpf.next_action_due_at IS NOT NULL THEN 1
                    ELSE 2 END,
                    sfpf.next_action_due_at ASC NULLS LAST,
                    sfpf.created_at ASC,
                    sfpf.id ASC
                LIMIT :limit OFFSET :offset
                """).params(pageParams).query(this::mapItem).list();
        return ServiceProfitWorkQueuePage.of(items, query.page(), query.size(), total);
    }

    private String buildPredicate(UUID tenantId, UUID principalId, List<AuthorizationGrant> grants,
                                  ServiceProfitWorkQueueQuery query, Map<String, Object> params) {
        List<String> predicates = new ArrayList<>();
        predicates.add("sfpf.tenant_id = :authenticatedTenantId");
        predicates.add(buildAuthorizationPredicate(tenantId, grants, params));
        params.put("handlingStatus", query.handlingStatus().name());
        predicates.add("sfpf.handling_status = :handlingStatus");
        if (query.ownership() == ServiceProfitWorkQueueOwnership.UNASSIGNED) predicates.add("sfpf.owner_principal_id IS NULL");
        if (query.ownership() == ServiceProfitWorkQueueOwnership.MINE) {
            params.put("authenticatedPrincipalId", principalId);
            predicates.add("sfpf.owner_principal_id = :authenticatedPrincipalId");
        }
        if (query.disposition() != null) {
            params.put("disposition", query.disposition().name());
            predicates.add("sfpf.current_disposition = :disposition");
        }
        switch (query.dueState()) {
            case OVERDUE -> predicates.add("sfpf.handling_status = 'OPEN' AND sfpf.next_action_due_at < CURRENT_TIMESTAMP");
            case UPCOMING -> predicates.add("sfpf.handling_status = 'OPEN' AND sfpf.next_action_due_at >= CURRENT_TIMESTAMP");
            case NO_DUE_DATE -> predicates.add("sfpf.next_action_due_at IS NULL");
            case ALL -> { }
        }
        return String.join(" AND ", predicates);
    }

    private String buildAuthorizationPredicate(UUID tenantId, List<AuthorizationGrant> grants,
                                               Map<String, Object> params) {
        List<String> predicates = new ArrayList<>();
        int index = 0;
        for (AuthorizationGrant grant : grants) {
            String parameter = "grant" + index++;
            switch (grant.scopeType()) {
                case TENANT -> { if (grant.scopeId().equals(tenantId)) predicates.add("TRUE"); }
                case TENANT_GROUP -> predicates.add("TRUE");
                case DEALER_GROUP -> {
                    params.put(parameter, grant.scopeId());
                    predicates.add("EXISTS (SELECT 1 FROM platform.dealer_group_memberships dgm WHERE dgm.tenant_id = sfpf.tenant_id AND dgm.dealer_group_id = :%s AND dgm.dealer_id = spo.dealer_id)".formatted(parameter));
                }
                case DEALER -> { params.put(parameter, grant.scopeId()); predicates.add("spo.dealer_id = :" + parameter); }
                case BRANCH -> { params.put(parameter, grant.scopeId()); predicates.add("spo.branch_id = :" + parameter); }
                case LOCATION -> { params.put(parameter, grant.scopeId()); predicates.add("spo.location_id = :" + parameter); }
            }
        }
        return predicates.isEmpty() ? "FALSE" : "(" + String.join(" OR ", predicates) + ")";
    }

    private ServiceProfitWorkQueueItem mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceProfitWorkQueueItem(
                rs.getObject("follow_up_id", UUID.class), rs.getObject("opportunity_id", UUID.class),
                ServiceProfitFollowUpHandlingStatus.valueOf(rs.getString("handling_status")),
                rs.getObject("owner_principal_id", UUID.class), rs.getObject("claimed_at", java.time.OffsetDateTime.class),
                rs.getObject("next_action_due_at", java.time.OffsetDateTime.class),
                ServiceProfitFollowUpDisposition.valueOf(rs.getString("current_disposition")),
                rs.getLong("follow_up_version"), rs.getObject("follow_up_created_at", java.time.OffsetDateTime.class),
                rs.getObject("follow_up_updated_at", java.time.OffsetDateTime.class),
                ServiceProfitActionability.valueOf(rs.getString("actionability")),
                ServiceProfitOpportunityStatus.valueOf(rs.getString("opportunity_status")),
                ServiceProfitEvidenceClass.valueOf(rs.getString("evidence_class")),
                ServiceProfitEvidenceStrength.valueOf(rs.getString("evidence_strength")),
                ServiceProfitPriority.valueOf(rs.getString("priority")), rs.getString("title"), rs.getString("summary"));
    }
}