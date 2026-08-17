package com.autovision.platform.aftersales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AfterSalesCaseRepository extends JpaRepository<AfterSalesCase, UUID> {

    List<AfterSalesCase> findAllByTenantId(UUID tenantId);

    List<AfterSalesCase> findAllByTenantIdAndLifecycleStatus(
            UUID tenantId,
            String lifecycleStatus
    );

    Optional<AfterSalesCase> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    Optional<AfterSalesCase> findByTenantIdAndCaseNumber(
            UUID tenantId,
            String caseNumber
    );

    boolean existsByTenantIdAndCaseNumber(
            UUID tenantId,
            String caseNumber
    );
}
