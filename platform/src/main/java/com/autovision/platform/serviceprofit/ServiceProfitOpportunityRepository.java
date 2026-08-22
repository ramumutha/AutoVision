package com.autovision.platform.serviceprofit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceProfitOpportunityRepository
        extends JpaRepository<ServiceProfitOpportunity, UUID> {

    List<ServiceProfitOpportunity> findAllByTenantId(
            UUID tenantId
    );

    List<ServiceProfitOpportunity> findAllByTenantIdAndStatus(
            UUID tenantId,
            ServiceProfitOpportunityStatus status
    );

    List<ServiceProfitOpportunity> findAllByTenantIdAndPriorityAndStatus(
            UUID tenantId,
            ServiceProfitPriority priority,
            ServiceProfitOpportunityStatus status
    );

    Optional<ServiceProfitOpportunity> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    Optional<ServiceProfitOpportunity> findByTenantIdAndOpportunityKey(
            UUID tenantId,
            String opportunityKey
    );

    boolean existsByTenantIdAndOpportunityKey(
            UUID tenantId,
            String opportunityKey
    );
}
