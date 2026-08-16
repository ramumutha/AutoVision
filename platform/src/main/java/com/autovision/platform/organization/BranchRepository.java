package com.autovision.platform.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findAllByTenantId(UUID tenantId);

    List<Branch> findAllByTenantIdAndDealerId(UUID tenantId, UUID dealerId);

    Optional<Branch> findByIdAndTenantId(UUID id, UUID tenantId);

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