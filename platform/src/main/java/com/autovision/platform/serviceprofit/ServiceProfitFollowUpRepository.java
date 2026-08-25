package com.autovision.platform.serviceprofit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceProfitFollowUpRepository
        extends JpaRepository<ServiceProfitFollowUp, UUID> {

    Optional<ServiceProfitFollowUp> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ServiceProfitFollowUp> findByTenantIdAndOpportunityId(
            UUID tenantId,
            UUID opportunityId
    );
}