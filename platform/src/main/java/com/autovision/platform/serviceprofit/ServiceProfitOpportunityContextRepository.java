package com.autovision.platform.serviceprofit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceProfitOpportunityContextRepository
        extends JpaRepository<ServiceProfitOpportunityContext, UUID> {

    Optional<ServiceProfitOpportunityContext> findByOpportunityIdAndTenantId(
            UUID opportunityId,
            UUID tenantId
    );
}