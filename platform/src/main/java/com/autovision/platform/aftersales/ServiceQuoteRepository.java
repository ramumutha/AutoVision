package com.autovision.platform.aftersales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceQuoteRepository
        extends JpaRepository<ServiceQuote, UUID> {

    Optional<ServiceQuote> findByIdAndTenantId(UUID id, UUID tenantId);

    List<ServiceQuote> findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(
            UUID serviceOrderId,
            UUID tenantId
    );

    List<ServiceQuote> findByAfterSalesCaseIdAndTenantIdOrderByCreatedAtAsc(
            UUID afterSalesCaseId,
            UUID tenantId
    );

    boolean existsByTenantIdAndQuoteNumber(UUID tenantId, String quoteNumber);
}