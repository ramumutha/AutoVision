package com.autovision.platform.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealerRepository extends JpaRepository<Dealer, UUID> {

    List<Dealer> findAllByTenantId(UUID tenantId);

    List<Dealer> findAllByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);


    /**
     * Returns resources belonging to tenants in the specified ACTIVE tenant
     * group. The authenticated tenant must itself be a member of that group.
     * This is the defense-in-depth collection boundary for TENANT_GROUP grants.
     */
    @Query(value = """
            SELECT DISTINCT d.*
              FROM platform.dealers d
              JOIN platform.tenant_group_memberships target_m
                ON target_m.tenant_id = d.tenant_id
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
    List<Dealer> findAllWithinActiveTenantGroup(
            @Param("tenantGroupId") UUID tenantGroupId,
            @Param("authenticatedTenantId") UUID authenticatedTenantId
    );

    Optional<Dealer> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Defense-in-depth boundary for resource-by-ID reads. The primary
     * authorization decision is made before this query executes. This query
     * then constrains loading to either the authenticated tenant or a target
     * tenant that shares an ACTIVE tenant group with the authenticated tenant.
     */
    @Query(value = """
            SELECT d.*
              FROM platform.dealers d
             WHERE d.id = :id
               AND (
                    d.tenant_id = :authenticatedTenantId
                    OR EXISTS (
                        SELECT 1
                          FROM platform.tenant_group_memberships caller_m
                          JOIN platform.tenant_groups tg
                            ON tg.id = caller_m.tenant_group_id
                           AND tg.status = 'ACTIVE'
                          JOIN platform.tenant_group_memberships target_m
                            ON target_m.tenant_group_id = tg.id
                         WHERE caller_m.tenant_id = :authenticatedTenantId
                           AND target_m.tenant_id = d.tenant_id
                    )
               )
            """, nativeQuery = true)
    Optional<Dealer> findByIdWithinAuthorizedTenantBoundary(
            @Param("id") UUID id,
            @Param("authenticatedTenantId") UUID authenticatedTenantId
    );

    Optional<Dealer> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}