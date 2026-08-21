package com.autovision.platform.aftersales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceQuoteLineRepository
        extends JpaRepository<ServiceQuoteLine, UUID> {

    List<ServiceQuoteLine> findByServiceQuoteIdOrderBySequenceAsc(
            UUID serviceQuoteId
    );

    List<ServiceQuoteLine>
    findAllByServiceQuoteIdInOrderByServiceQuoteIdAscSequenceAsc(
            List<UUID> serviceQuoteIds
    );

    Optional<ServiceQuoteLine> findByIdAndServiceQuoteId(
            UUID id,
            UUID serviceQuoteId
    );

    boolean existsByServiceQuoteIdAndServiceLineId(
            UUID serviceQuoteId,
            UUID serviceLineId
    );

    @Query("""
            select quote.status
            from ServiceQuoteLine quoteLine
            join ServiceQuote quote on quote.id = quoteLine.serviceQuoteId
            where quoteLine.serviceLineId = :serviceLineId
              and quote.serviceOrderId = :serviceOrderId
              and quote.tenantId = :tenantId
            """)
    List<ServiceQuoteStatus>
    findStatusesByServiceLineIdAndServiceOrderIdAndTenantId(
            @Param("serviceLineId") UUID serviceLineId,
            @Param("serviceOrderId") UUID serviceOrderId,
            @Param("tenantId") UUID tenantId
    );
}