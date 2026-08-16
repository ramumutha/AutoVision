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

    Optional<Location> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Location> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}