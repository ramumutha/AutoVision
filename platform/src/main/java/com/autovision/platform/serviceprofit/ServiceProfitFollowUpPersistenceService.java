package com.autovision.platform.serviceprofit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
/** Creates the one durable internal follow-up work item for an opportunity. */
public class ServiceProfitFollowUpPersistenceService {

    private final ServiceProfitOpportunityRepository opportunityRepository;
    private final ServiceProfitFollowUpRepository followUpRepository;
    private final ServiceProfitFollowUpHistoryRepository historyRepository;

    public ServiceProfitFollowUpPersistenceService(
            ServiceProfitOpportunityRepository opportunityRepository,
            ServiceProfitFollowUpRepository followUpRepository,
            ServiceProfitFollowUpHistoryRepository historyRepository
    ) {
        this.opportunityRepository = opportunityRepository;
        this.followUpRepository = followUpRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public ServiceProfitFollowUp create(
            UUID tenantId,
            UUID opportunityId,
            UUID actorPrincipalId,
            OffsetDateTime now
    ) {
        require(tenantId, "Tenant ID is required");
        require(opportunityId, "Opportunity ID is required");
        require(actorPrincipalId, "Actor principal ID is required");
        if (now == null) throw new IllegalArgumentException("Creation time is required");

        ServiceProfitOpportunity opportunity = opportunityRepository
                .findByIdAndTenantId(opportunityId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found in tenant"));
        var existingFollowUp = followUpRepository.findByTenantIdAndOpportunityId(
                tenantId,
                opportunityId
        );
        if (existingFollowUp.isPresent()) {
            return existingFollowUp.get();
        }

        ServiceProfitFollowUp followUp = followUpRepository.save(
                ServiceProfitFollowUp.open(UUID.randomUUID(), opportunity.getTenantId(), opportunity.getId(), now)
        );
        historyRepository.save(ServiceProfitFollowUpHistory.record(
                UUID.randomUUID(), tenantId, followUp.getId(), opportunityId, actorPrincipalId,
                ServiceProfitFollowUpEventType.CREATED, null,
                ServiceProfitFollowUpHandlingStatus.OPEN.name(), 0, followUp.getVersion(), now
        ));
        return followUp;
    }

    private void require(UUID value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }
}