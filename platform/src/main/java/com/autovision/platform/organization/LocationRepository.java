package com.autovision.platform.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    List<Location> findAllByTenantId(UUID tenantId);

    List<Location> findAllByTenantIdAndIdIn(
            UUID tenantId,
            Collection<UUID> ids
    );

    /**
 * Locations operationally contained by any of the given dealers
 * through their branches.
 */
@Query("""
        SELECT DISTINCT l FROM Location l
         WHERE l.tenantId = :tenantId
           AND l.id IN (
               SELECT b.locationId FROM Branch b
                WHERE b.tenantId = :tenantId
                  AND b.dealerId IN :dealerIds
                  AND b.locationId IS NOT NULL
           )
        """)

    List<Location> findAllByTenantIdAndDealerIdIn(
            @Param("tenantId") UUID tenantId,
            @Param("dealerIds") Collection<UUID> dealerIds
    );

    /**
     * Locations contained by any of the given branches.
     */
    @Query("""
            SELECT l FROM Location l
             WHERE l.tenantId = :tenantId
               AND l.id IN (
                   SELECT b.locationId FROM Branch b
                    WHERE b.tenantId = :tenantId
                      AND b.id IN :branchIds
                      AND b.locationId IS NOT NULL
               )
            """)
    List<Location> findAllByTenantIdAndBranchIdIn(
            @Param("tenantId") UUID tenantId,
            @Param("branchIds") Collection<UUID> branchIds
    );


    /**
     * Returns resources belonging to tenants in the specified ACTIVE tenant
     * group. The authenticated tenant must itself be a member of that group.
     * This is the defense-in-depth collection boundary for TENANT_GROUP grants.
     */
    @Query(value = """
            SELECT DISTINCT l.*
              FROM platform.locations l
              JOIN platform.tenant_group_memberships target_m
                ON target_m.tenant_id = l.tenant_id
              JOIN platform.tenant_groups tg
                ON tg.id = target_m.tenant_group_id
               AND tg.status = 'ACTIVE'
             WHERE tg.id = :tenantGroupId
               AND EXISTS (
                    SELECT 1
                      FROM platform.tenant_group_memberships caller_m
                     WHERE caller_m.tenant_group_id = tg.id
                       AND caller_m.tenant_id = :authenticatedTenantId
               )
            """, nativeQuery = true)
    List<Location> findAllWithinActiveTenantGroup(
            @Param("tenantGroupId") UUID tenantGroupId,
            @Param("authenticatedTenantId") UUID authenticatedTenantId
    );

    Optional<Location> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Defense-in-depth boundary for resource-by-ID reads. The primary
     * authorization decision is made before this query executes. This query
     * then constrains loading to either the authenticated tenant or a target
     * tenant that shares an ACTIVE tenant group with the authenticated tenant.
     */
    @Query(value = """
            SELECT l.*
              FROM platform.locations l
             WHERE l.id = :id
               AND (
                    l.tenant_id = :authenticatedTenantId
                    OR EXISTS (
                        SELECT 1
                          FROM platform.tenant_group_memberships caller_m
                          JOIN platform.tenant_groups tg
                            ON tg.id = caller_m.tenant_group_id
                           AND tg.status = 'ACTIVE'
                          JOIN platform.tenant_group_memberships target_m
                            ON target_m.tenant_group_id = tg.id
                         WHERE caller_m.tenant_id = :authenticatedTenantId
                           AND target_m.tenant_id = l.tenant_id
                    )
               )
            """, nativeQuery = true)
    Optional<Location> findByIdWithinAuthorizedTenantBoundary(
            @Param("id") UUID id,
            @Param("authenticatedTenantId") UUID authenticatedTenantId
    );

    Optional<Location> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}