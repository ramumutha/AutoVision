package com.autovision.platform.serviceprofit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceProfitFollowUpHistoryRepository
        extends JpaRepository<ServiceProfitFollowUpHistory, UUID> {

    List<ServiceProfitFollowUpHistory> findAllByTenantIdAndFollowUpIdOrderByOccurredAtAscIdAsc(
            UUID tenantId,
            UUID followUpId
    );
}