package com.autovision.platform.authorization;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tenant-aware containment existence checks used by AuthorizationScopeEvaluator.
 * Performs boolean/count existence queries only; never loads domain objects.
 */
@Repository
public class AuthorizationScopeRepository {

    private final JdbcClient jdbcClient;

    public AuthorizationScopeRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }


    public boolean tenantBelongsToActiveTenantGroup(
            UUID tenantId,
            UUID tenantGroupId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.tenant_groups tg
                  JOIN platform.tenant_group_memberships m
                    ON m.tenant_group_id = tg.id
                 WHERE tg.id = :tenantGroupId
                   AND tg.status = 'ACTIVE'
                   AND m.tenant_id = :tenantId
                """)
                .param("tenantGroupId", tenantGroupId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean dealerBelongsToActiveTenantGroup(
            UUID dealerId,
            UUID tenantGroupId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.dealers d
                  JOIN platform.tenant_group_memberships m
                    ON m.tenant_id = d.tenant_id
                  JOIN platform.tenant_groups tg
                    ON tg.id = m.tenant_group_id
                 WHERE d.id = :dealerId
                   AND tg.id = :tenantGroupId
                   AND tg.status = 'ACTIVE'
                """)
                .param("dealerId", dealerId)
                .param("tenantGroupId", tenantGroupId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean branchBelongsToActiveTenantGroup(
            UUID branchId,
            UUID tenantGroupId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.branches b
                  JOIN platform.tenant_group_memberships m
                    ON m.tenant_id = b.tenant_id
                  JOIN platform.tenant_groups tg
                    ON tg.id = m.tenant_group_id
                 WHERE b.id = :branchId
                   AND tg.id = :tenantGroupId
                   AND tg.status = 'ACTIVE'
                """)
                .param("branchId", branchId)
                .param("tenantGroupId", tenantGroupId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean locationBelongsToActiveTenantGroup(
            UUID locationId,
            UUID tenantGroupId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.locations l
                  JOIN platform.tenant_group_memberships m
                    ON m.tenant_id = l.tenant_id
                  JOIN platform.tenant_groups tg
                    ON tg.id = m.tenant_group_id
                 WHERE l.id = :locationId
                   AND tg.id = :tenantGroupId
                   AND tg.status = 'ACTIVE'
                """)
                .param("locationId", locationId)
                .param("tenantGroupId", tenantGroupId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean dealerBelongsToTenant(UUID dealerId, UUID tenantId) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.dealers
                 WHERE id = :dealerId
                   AND tenant_id = :tenantId
                """,
                "dealerId", dealerId,
                "tenantId", tenantId
        );
    }

    public boolean branchBelongsToTenant(UUID branchId, UUID tenantId) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.branches
                 WHERE id = :branchId
                   AND tenant_id = :tenantId
                """,
                "branchId", branchId,
                "tenantId", tenantId
        );
    }

    public boolean locationBelongsToTenant(UUID locationId, UUID tenantId) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.locations
                 WHERE id = :locationId
                   AND tenant_id = :tenantId
                """,
                "locationId", locationId,
                "tenantId", tenantId
        );
    }

    public boolean dealerIsMemberOfGroup(
            UUID dealerId,
            UUID dealerGroupId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.dealer_group_memberships
                 WHERE dealer_id = :dealerId
                   AND dealer_group_id = :dealerGroupId
                   AND tenant_id = :tenantId
                """)
                .param("dealerId", dealerId)
                .param("dealerGroupId", dealerGroupId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    /**
     * Member dealer IDs for a group, used by collection-scoped findAll
     * filtering (S4.7.7.3D). Membership is always tenant-scoped.
     */
    public List<UUID> findDealerIdsInGroup(
            UUID dealerGroupId,
            UUID tenantId
    ) {
        return jdbcClient.sql("""
                SELECT DISTINCT dealer_id
                  FROM platform.dealer_group_memberships
                 WHERE dealer_group_id = :dealerGroupId
                   AND tenant_id = :tenantId
                """)
                .param("dealerGroupId", dealerGroupId)
                .param("tenantId", tenantId)
                .query(UUID.class)
                .list();
    }

    public boolean branchBelongsToDealer(
            UUID branchId,
            UUID dealerId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.branches
                 WHERE id = :branchId
                   AND dealer_id = :dealerId
                   AND tenant_id = :tenantId
                """)
                .param("branchId", branchId)
                .param("dealerId", dealerId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    /**
     * A location belongs to a dealer only through an operational branch
     * relationship. The dealer primary-location reference is a business
     * attribute and does not establish authorization containment.
     */
    public boolean locationBelongsToDealer(
            UUID locationId,
            UUID dealerId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.branches
                 WHERE location_id = :locationId
                   AND dealer_id = :dealerId
                   AND tenant_id = :tenantId
                """)
                .param("locationId", locationId)
                .param("dealerId", dealerId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean locationBelongsToBranch(
            UUID locationId,
            UUID branchId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.branches
                 WHERE id = :branchId
                   AND location_id = :locationId
                   AND tenant_id = :tenantId
                """)
                .param("branchId", branchId)
                .param("locationId", locationId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean branchBelongsToDealerGroup(
            UUID branchId,
            UUID dealerGroupId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.branches b
                  JOIN platform.dealer_group_memberships m
                    ON m.dealer_id = b.dealer_id
                   AND m.tenant_id = b.tenant_id
                 WHERE b.id = :branchId
                   AND m.dealer_group_id = :dealerGroupId
                   AND b.tenant_id = :tenantId
                """)
                .param("branchId", branchId)
                .param("dealerGroupId", dealerGroupId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    /**
     * A location belongs to a dealer group only through a branch of a
     * member dealer. The dealer primary-location reference does not establish
     * authorization containment.
     */
    public boolean locationBelongsToDealerGroup(
            UUID locationId,
            UUID dealerGroupId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.branches b
                  JOIN platform.dealer_group_memberships m
                    ON m.dealer_id = b.dealer_id
                   AND m.tenant_id = b.tenant_id
                 WHERE b.location_id = :locationId
                   AND m.dealer_group_id = :dealerGroupId
                   AND b.tenant_id = :tenantId
                """)
                .param("locationId", locationId)
                .param("dealerGroupId", dealerGroupId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean afterSalesCaseBelongsToTenant(
            UUID caseId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.aftersales_cases
                 WHERE id = :caseId
                   AND tenant_id = :tenantId
                """,
                "caseId", caseId,
                "tenantId", tenantId
        );
    }

    public boolean afterSalesCaseBelongsToDealer(
            UUID caseId,
            UUID dealerId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.aftersales_cases
                 WHERE id = :caseId
                   AND dealer_id = :dealerId
                   AND tenant_id = :tenantId
                """)
                .param("caseId", caseId)
                .param("dealerId", dealerId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean afterSalesCaseBelongsToBranch(
            UUID caseId,
            UUID branchId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.aftersales_cases
                 WHERE id = :caseId
                   AND branch_id = :branchId
                   AND tenant_id = :tenantId
                """)
                .param("caseId", caseId)
                .param("branchId", branchId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean afterSalesCaseBelongsToDealerGroup(
            UUID caseId,
            UUID dealerGroupId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.aftersales_cases c
                  JOIN platform.dealer_group_memberships m
                    ON m.dealer_id = c.dealer_id
                   AND m.tenant_id = c.tenant_id
                 WHERE c.id = :caseId
                   AND m.dealer_group_id = :dealerGroupId
                   AND c.tenant_id = :tenantId
                """)
                .param("caseId", caseId)
                .param("dealerGroupId", dealerGroupId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean afterSalesCaseBelongsToActiveTenantGroup(
            UUID caseId,
            UUID tenantGroupId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.aftersales_cases c
                  JOIN platform.tenant_group_memberships m
                    ON m.tenant_id = c.tenant_id
                  JOIN platform.tenant_groups tg
                    ON tg.id = m.tenant_group_id
                 WHERE c.id = :caseId
                   AND tg.id = :tenantGroupId
                   AND tg.status = 'ACTIVE'
                """)
                .param("caseId", caseId)
                .param("tenantGroupId", tenantGroupId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }
    public boolean serviceOrderBelongsToTenant(
            UUID serviceOrderId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_orders
                 WHERE id = :serviceOrderId
                   AND tenant_id = :tenantId
                """,
                "serviceOrderId", serviceOrderId,
                "tenantId", tenantId
        );
    }

    public boolean serviceOrderBelongsToDealer(
            UUID serviceOrderId,
            UUID dealerId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.service_orders
                 WHERE id = :serviceOrderId
                   AND dealer_id = :dealerId
                   AND tenant_id = :tenantId
                """)
                .param("serviceOrderId", serviceOrderId)
                .param("dealerId", dealerId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean serviceOrderBelongsToBranch(
            UUID serviceOrderId,
            UUID branchId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.service_orders
                 WHERE id = :serviceOrderId
                   AND branch_id = :branchId
                   AND tenant_id = :tenantId
                """)
                .param("serviceOrderId", serviceOrderId)
                .param("branchId", branchId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean serviceOrderBelongsToDealerGroup(
            UUID serviceOrderId,
            UUID dealerGroupId,
            UUID tenantId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.service_orders so
                  JOIN platform.dealer_group_memberships m
                    ON m.dealer_id = so.dealer_id
                   AND m.tenant_id = so.tenant_id
                 WHERE so.id = :serviceOrderId
                   AND m.dealer_group_id = :dealerGroupId
                   AND so.tenant_id = :tenantId
                """)
                .param("serviceOrderId", serviceOrderId)
                .param("dealerGroupId", dealerGroupId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean serviceOrderBelongsToActiveTenantGroup(
            UUID serviceOrderId,
            UUID tenantGroupId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.service_orders so
                  JOIN platform.tenant_group_memberships m
                    ON m.tenant_id = so.tenant_id
                  JOIN platform.tenant_groups tg
                    ON tg.id = m.tenant_group_id
                 WHERE so.id = :serviceOrderId
                   AND tg.id = :tenantGroupId
                   AND tg.status = 'ACTIVE'
                """)
                .param("serviceOrderId", serviceOrderId)
                .param("tenantGroupId", tenantGroupId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    public boolean serviceWorkflowBelongsToTenant(
            UUID workflowId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_workflow_definitions
                 WHERE id = :workflowId
                   AND tenant_id = :tenantId
                """,
                "workflowId", workflowId,
                "tenantId", tenantId
        );
    }

    public boolean serviceWorkflowBelongsToDealer(
            UUID workflowId,
            UUID dealerId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_workflow_definitions
                 WHERE id = :workflowId
                   AND dealer_id = :dealerId
                   AND tenant_id = :tenantId
                """,
                "workflowId", workflowId,
                "dealerId", dealerId,
                "tenantId", tenantId
        );
    }

    public boolean serviceWorkflowBelongsToBranch(
            UUID workflowId,
            UUID branchId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_workflow_definitions
                 WHERE id = :workflowId
                   AND branch_id = :branchId
                   AND tenant_id = :tenantId
                """,
                "workflowId", workflowId,
                "branchId", branchId,
                "tenantId", tenantId
        );
    }

    public boolean serviceWorkflowBelongsToDealerGroup(
            UUID workflowId,
            UUID dealerGroupId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_workflow_definitions wf
                  JOIN platform.dealer_group_memberships m
                    ON m.dealer_id = wf.dealer_id
                   AND m.tenant_id = wf.tenant_id
                 WHERE wf.id = :workflowId
                   AND m.dealer_group_id = :dealerGroupId
                   AND wf.tenant_id = :tenantId
                """,
                "workflowId", workflowId,
                "dealerGroupId", dealerGroupId,
                "tenantId", tenantId
        );
    }

    public boolean serviceWorkflowBelongsToActiveTenantGroup(
            UUID workflowId,
            UUID tenantGroupId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_workflow_definitions wf
                  JOIN platform.tenant_group_memberships m
                    ON m.tenant_id = wf.tenant_id
                  JOIN platform.tenant_groups tg
                    ON tg.id = m.tenant_group_id
                 WHERE wf.id = :workflowId
                   AND tg.id = :tenantGroupId
                   AND tg.status = 'ACTIVE'
                """,
                "workflowId", workflowId,
                "tenantGroupId", tenantGroupId
        );
    }


    public boolean serviceProfitOpportunityBelongsToTenant(
            UUID opportunityId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_profit_opportunities
                 WHERE id = :opportunityId
                   AND tenant_id = :tenantId
                """,
                "opportunityId", opportunityId,
                "tenantId", tenantId
        );
    }

    public boolean serviceProfitOpportunityBelongsToDealer(
            UUID opportunityId,
            UUID dealerId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_profit_opportunities
                 WHERE id = :opportunityId
                   AND dealer_id = :dealerId
                   AND tenant_id = :tenantId
                """,
                "opportunityId", opportunityId,
                "dealerId", dealerId,
                "tenantId", tenantId
        );
    }

    public boolean serviceProfitOpportunityBelongsToBranch(
            UUID opportunityId,
            UUID branchId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_profit_opportunities
                 WHERE id = :opportunityId
                   AND branch_id = :branchId
                   AND tenant_id = :tenantId
                """,
                "opportunityId", opportunityId,
                "branchId", branchId,
                "tenantId", tenantId
        );
    }

    public boolean serviceProfitOpportunityBelongsToLocation(
            UUID opportunityId,
            UUID locationId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_profit_opportunities
                 WHERE id = :opportunityId
                   AND location_id = :locationId
                   AND tenant_id = :tenantId
                """,
                "opportunityId", opportunityId,
                "locationId", locationId,
                "tenantId", tenantId
        );
    }

    public boolean serviceProfitOpportunityBelongsToDealerGroup(
            UUID opportunityId,
            UUID dealerGroupId,
            UUID tenantId
    ) {
        return exists("""
                SELECT COUNT(*)
                  FROM platform.service_profit_opportunities spo
                  JOIN platform.dealer_group_memberships m
                    ON m.dealer_id = spo.dealer_id
                   AND m.tenant_id = spo.tenant_id
                 WHERE spo.id = :opportunityId
                   AND m.dealer_group_id = :dealerGroupId
                   AND spo.tenant_id = :tenantId
                """,
                "opportunityId", opportunityId,
                "dealerGroupId", dealerGroupId,
                "tenantId", tenantId
        );
    }

    public boolean serviceProfitOpportunityBelongsToActiveTenantGroup(
            UUID opportunityId,
            UUID tenantGroupId
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.service_profit_opportunities spo
                  JOIN platform.tenant_group_memberships m
                    ON m.tenant_id = spo.tenant_id
                  JOIN platform.tenant_groups tg
                    ON tg.id = m.tenant_group_id
                 WHERE spo.id = :opportunityId
                   AND tg.id = :tenantGroupId
                   AND tg.status = 'ACTIVE'
                """)
                .param("opportunityId", opportunityId)
                .param("tenantGroupId", tenantGroupId)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }
    private boolean exists(
            String sql,
            String param1Name,
            UUID param1Value,
            String param2Name,
            UUID param2Value
    ) {
        Integer matchCount = jdbcClient.sql(sql)
                .param(param1Name, param1Value)
                .param(param2Name, param2Value)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

        private boolean exists(
                        String sql,
                        String param1Name,
                        UUID param1Value,
                        String param2Name,
                        UUID param2Value,
                        String param3Name,
                        UUID param3Value
        ) {
                Integer matchCount = jdbcClient.sql(sql)
                                .param(param1Name, param1Value)
                                .param(param2Name, param2Value)
                                .param(param3Name, param3Value)
                                .query(Integer.class)
                                .single();

                return matchCount != null && matchCount > 0;
        }
}

