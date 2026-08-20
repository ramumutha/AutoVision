package com.autovision.platform.aftersales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceQuoteLineRepository
        extends JpaRepository<ServiceQuoteLine, UUID> {

    List<ServiceQuoteLine> findByServiceQuoteIdOrderBySequenceAsc(
            UUID serviceQuoteId
    );

    Optional<ServiceQuoteLine> findByIdAndServiceQuoteId(
            UUID id,
            UUID serviceQuoteId
    );

    boolean existsByServiceQuoteIdAndServiceLineId(
            UUID serviceQuoteId,
            UUID serviceLineId
    );
}