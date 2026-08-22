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
public class ServiceProfitOpportunityQueryRepository {

    private final JdbcClient jdbcClient;

    public ServiceProfitOpportunityQueryRepository(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public ServiceProfitOpportunityPage findAuthorizedPage(
            UUID authenticatedTenantId,
            List<AuthorizationGrant> grants,
            ServiceProfitOpportunityQuery query
    ) {
        Map<String, Object> params = new HashMap<>();

        params.put(
                "authenticatedTenantId",
                authenticatedTenantId
        );

        String predicate = buildPredicate(
                authenticatedTenantId,
                grants,
                query,
                params
        );

        String from = """
                FROM platform.service_profit_opportunities spo
                WHERE
                """ + predicate;

        Long total = jdbcClient.sql(
                        "SELECT COUNT(*) " + from
                )
                .params(params)
                .query(Long.class)
                .single();

        if (total == null || total == 0) {
            return ServiceProfitOpportunityPage.of(
                    List.of(),
                    query.page(),
                    query.size(),
                    0
            );
        }

        Map<String, Object> pageParams =
                new HashMap<>(params);

        pageParams.put("limit", query.size());
        pageParams.put("offset", query.offset());

        String sql = """
                SELECT
                    spo.id,
                    spo.tenant_id,
                    spo.dealer_id,
                    spo.branch_id,
                    spo.location_id,
                    spo.opportunity_key,
                    spo.opportunity_type,
                    spo.status,
                    spo.evidence_class,
                    spo.evidence_strength,
                    spo.priority,
                    spo.actionability,
                    spo.title,
                    spo.potential_amount,
                    spo.currency_code,
                    spo.detected_at
                """ +
                from +
                " ORDER BY " +
                orderBy(query.sort()) +
                " LIMIT :limit OFFSET :offset";

        List<ServiceProfitOpportunityQueueItem> items =
                jdbcClient.sql(sql)
                        .params(pageParams)
                        .query(this::mapQueueItem)
                        .list();

        return ServiceProfitOpportunityPage.of(
                items,
                query.page(),
                query.size(),
                total
        );
    }

    private String buildPredicate(
            UUID authenticatedTenantId,
            List<AuthorizationGrant> grants,
            ServiceProfitOpportunityQuery query,
            Map<String, Object> params
    ) {
        List<String> predicates = new ArrayList<>();

        /*
         * R1.0 deliberately keeps collection results inside the authenticated
         * tenant boundary. TENANT_GROUP cross-tenant portfolio views are
         * deferred to a separately designed executive aggregation capability.
         */
        predicates.add(
                "spo.tenant_id = :authenticatedTenantId"
        );

        predicates.add(
                buildAuthorizationPredicate(
                        authenticatedTenantId,
                        grants,
                        params
                )
        );

        if (query.status() != null) {
            params.put("status", query.status().name());
            predicates.add("spo.status = :status");
        }

        if (query.priority() != null) {
            params.put("priority", query.priority().name());
            predicates.add("spo.priority = :priority");
        }

        if (query.opportunityType() != null) {
            params.put(
                    "opportunityType",
                    query.opportunityType().name()
            );
            predicates.add(
                    "spo.opportunity_type = :opportunityType"
            );
        }

        if (query.evidenceClass() != null) {
            params.put(
                    "evidenceClass",
                    query.evidenceClass().name()
            );
            predicates.add(
                    "spo.evidence_class = :evidenceClass"
            );
        }

        if (query.evidenceStrength() != null) {
            params.put(
                    "evidenceStrength",
                    query.evidenceStrength().name()
            );
            predicates.add(
                    "spo.evidence_strength = :evidenceStrength"
            );
        }

        if (query.actionability() != null) {
            params.put(
                    "actionability",
                    query.actionability().name()
            );
            predicates.add(
                    "spo.actionability = :actionability"
            );
        }

        if (query.dealerId() != null) {
            params.put("dealerId", query.dealerId());
            predicates.add("spo.dealer_id = :dealerId");
        }

        if (query.branchId() != null) {
            params.put("branchId", query.branchId());
            predicates.add("spo.branch_id = :branchId");
        }

        if (query.locationId() != null) {
            params.put("locationId", query.locationId());
            predicates.add("spo.location_id = :locationId");
        }

        return String.join(
                System.lineSeparator() + " AND ",
                predicates
        );
    }

    private String buildAuthorizationPredicate(
            UUID authenticatedTenantId,
            List<AuthorizationGrant> grants,
            Map<String, Object> params
    ) {
        List<String> predicates = new ArrayList<>();

        int index = 0;

        for (AuthorizationGrant grant : grants) {
            String parameter = "grant" + index++;

            switch (grant.scopeType()) {
                case TENANT -> {
                    if (grant.scopeId().equals(
                            authenticatedTenantId
                    )) {
                        predicates.add("TRUE");
                    }
                }

                case TENANT_GROUP -> {
                    /*
                     * The caller has already been validated as an active
                     * TENANT_GROUP member by AuthorizationRepository.
                     * R1 queue remains authenticated-tenant-bound.
                     */
                    predicates.add("TRUE");
                }

                case DEALER_GROUP -> {
                    params.put(parameter, grant.scopeId());

                    predicates.add("""
                            EXISTS (
                                SELECT 1
                                  FROM platform.dealer_group_memberships dgm
                                 WHERE dgm.tenant_id = spo.tenant_id
                                   AND dgm.dealer_group_id = :%s
                                   AND dgm.dealer_id = spo.dealer_id
                            )
                            """.formatted(parameter));
                }

                case DEALER -> {
                    params.put(parameter, grant.scopeId());
                    predicates.add(
                            "spo.dealer_id = :" + parameter
                    );
                }

                case BRANCH -> {
                    params.put(parameter, grant.scopeId());
                    predicates.add(
                            "spo.branch_id = :" + parameter
                    );
                }

                case LOCATION -> {
                    params.put(parameter, grant.scopeId());
                    predicates.add(
                            "spo.location_id = :" + parameter
                    );
                }
            }
        }

        if (predicates.isEmpty()) {
            return "FALSE";
        }

        return "(" +
                String.join(" OR ", predicates) +
                ")";
    }

    private String orderBy(
            ServiceProfitOpportunitySort sort
    ) {
        return switch (sort) {
            case DETECTED_DESC ->
                    "spo.detected_at DESC, spo.id DESC";

            case DETECTED_ASC ->
                    "spo.detected_at ASC, spo.id ASC";

            case POTENTIAL_DESC ->
                    """
                    spo.potential_amount DESC NULLS LAST,
                    spo.detected_at DESC,
                    spo.id DESC
                    """;

            case POTENTIAL_ASC ->
                    """
                    spo.potential_amount ASC NULLS LAST,
                    spo.detected_at DESC,
                    spo.id DESC
                    """;
        };
    }

    private ServiceProfitOpportunityQueueItem mapQueueItem(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        return new ServiceProfitOpportunityQueueItem(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("dealer_id", UUID.class),
                rs.getObject("branch_id", UUID.class),
                rs.getObject("location_id", UUID.class),
                rs.getString("opportunity_key"),
                ServiceProfitOpportunityType.valueOf(
                        rs.getString("opportunity_type")
                ),
                ServiceProfitOpportunityStatus.valueOf(
                        rs.getString("status")
                ),
                ServiceProfitEvidenceClass.valueOf(
                        rs.getString("evidence_class")
                ),
                ServiceProfitEvidenceStrength.valueOf(
                        rs.getString("evidence_strength")
                ),
                ServiceProfitPriority.valueOf(
                        rs.getString("priority")
                ),
                ServiceProfitActionability.valueOf(
                        rs.getString("actionability")
                ),
                rs.getString("title"),
                rs.getBigDecimal("potential_amount"),
                rs.getString("currency_code"),
                rs.getObject(
                        "detected_at",
                        java.time.OffsetDateTime.class
                )
        );
    }
}
