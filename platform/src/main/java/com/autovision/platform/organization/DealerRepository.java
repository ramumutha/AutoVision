package com.autovision.platform.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DealerRepository extends JpaRepository<Dealer, UUID> {

    List<Dealer> findAllByTenantId(UUID tenantId);

    Optional<Dealer> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Dealer> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}