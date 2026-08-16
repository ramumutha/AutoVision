package com.autovision.platform.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    List<Location> findAllByTenantId(UUID tenantId);

    Optional<Location> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Location> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}