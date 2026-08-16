package com.autovision.platform.authorization;

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
     * A location belongs to a dealer either via a branch of that dealer
     * situated at the location, or as the dealer's primary location.
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
                UNION ALL
                SELECT COUNT(*)
                  FROM platform.dealers
                 WHERE id = :dealerId
                   AND primary_location_id = :locationId
                   AND tenant_id = :tenantId
                """)
                .param("locationId", locationId)
                .param("dealerId", dealerId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .list()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        return matchCount > 0;
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
     * A location belongs to a dealer group either via a branch of a member
     * dealer situated at the location, or as a member dealer's primary location.
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
                UNION ALL
                SELECT COUNT(*)
                  FROM platform.dealers d
                  JOIN platform.dealer_group_memberships m
                    ON m.dealer_id = d.id
                   AND m.tenant_id = d.tenant_id
                 WHERE d.primary_location_id = :locationId
                   AND m.dealer_group_id = :dealerGroupId
                   AND d.tenant_id = :tenantId
                """)
                .param("locationId", locationId)
                .param("dealerGroupId", dealerGroupId)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .list()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        return matchCount > 0;
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
