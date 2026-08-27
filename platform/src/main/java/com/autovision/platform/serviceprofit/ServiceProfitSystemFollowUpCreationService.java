package com.autovision.platform.serviceprofit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
class ServiceProfitSystemFollowUpCreationService {

    private final ServiceProfitFollowUpRepository followUpRepository;
    private final ServiceProfitFollowUpHistoryRepository historyRepository;

    ServiceProfitSystemFollowUpCreationService(
            ServiceProfitFollowUpRepository followUpRepository,
            ServiceProfitFollowUpHistoryRepository historyRepository
    ) {
        this.followUpRepository = followUpRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    ServiceProfitFollowUp create(
            UUID tenantId,
            UUID opportunityId,
            OffsetDateTime now
    ) {
        ServiceProfitFollowUp followUp = followUpRepository.saveAndFlush(
                ServiceProfitFollowUp.open(UUID.randomUUID(), tenantId, opportunityId, now)
        );
        historyRepository.save(ServiceProfitFollowUpHistory.recordSystem(
                UUID.randomUUID(), tenantId, followUp.getId(), opportunityId,
                ServiceProfitFollowUpEventType.CREATED, null,
                ServiceProfitFollowUpHandlingStatus.OPEN.name(), 0, followUp.getVersion(), now
        ));
        return followUp;
    }
}