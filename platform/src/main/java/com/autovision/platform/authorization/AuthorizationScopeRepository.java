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
}
