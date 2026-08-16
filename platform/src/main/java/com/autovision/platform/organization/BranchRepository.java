package com.autovision.platform.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findAllByTenantId(UUID tenantId);

    List<Branch> findAllByTenantIdAndDealerId(UUID tenantId, UUID dealerId);

    List<Branch> findAllByTenantIdAndDealerIdIn(
            UUID tenantId,
            Collection<UUID> dealerIds
    );

    List<Branch> findAllByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    Optional<Branch> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Defense-in-depth boundary for resource-by-ID reads. The primary
     * authorization decision is made before this query executes. This query
     * then constrains loading to either the authenticated tenant or a target
     * tenant that shares an ACTIVE tenant group with the authenticated tenant.
     */
    @Query(value = """
            SELECT b.*
              FROM platform.branches b
             WHERE b.id = :id
               AND (
                    b.tenant_id = :authenticatedTenantId
                    OR EXISTS (
                        SELECT 1
                          FROM platform.tenant_group_memberships caller_m
                          JOIN platform.tenant_groups tg
                            ON tg.id = caller_m.tenant_group_id
                           AND tg.status = 'ACTIVE'
                          JOIN platform.tenant_group_memberships target_m
                            ON target_m.tenant_group_id = tg.id
                         WHERE caller_m.tenant_id = :authenticatedTenantId
                           AND target_m.tenant_id = b.tenant_id
                    )
               )
            """, nativeQuery = true)
    Optional<Branch> findByIdWithinAuthorizedTenantBoundary(
            @Param("id") UUID id,
            @Param("authenticatedTenantId") UUID authenticatedTenantId
    );

    Optional<Branch> findByTenantIdAndDealerIdAndCode(
            UUID tenantId,
            UUID dealerId,
            String code
    );

    boolean existsByTenantIdAndDealerIdAndCode(
            UUID tenantId,
            UUID dealerId,
            String code
    );
}