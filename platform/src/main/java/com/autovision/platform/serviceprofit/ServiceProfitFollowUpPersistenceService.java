package com.autovision.platform.serviceprofit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
/** Creates the one durable internal follow-up work item for an opportunity. */
public class ServiceProfitFollowUpPersistenceService {

    private final ServiceProfitOpportunityRepository opportunityRepository;
    private final ServiceProfitFollowUpRepository followUpRepository;
    private final ServiceProfitFollowUpHistoryRepository historyRepository;
        private final ServiceProfitSystemFollowUpCreationService systemCreationService;

        @org.springframework.beans.factory.annotation.Autowired
    public ServiceProfitFollowUpPersistenceService(
            ServiceProfitOpportunityRepository opportunityRepository,
            ServiceProfitFollowUpRepository followUpRepository,
                        ServiceProfitFollowUpHistoryRepository historyRepository,
                        ServiceProfitSystemFollowUpCreationService systemCreationService
    ) {
        this.opportunityRepository = opportunityRepository;
        this.followUpRepository = followUpRepository;
        this.historyRepository = historyRepository;
                this.systemCreationService = systemCreationService;
        }

        public ServiceProfitFollowUpPersistenceService(
                        ServiceProfitOpportunityRepository opportunityRepository,
                        ServiceProfitFollowUpRepository followUpRepository,
                        ServiceProfitFollowUpHistoryRepository historyRepository
        ) {
                this(opportunityRepository, followUpRepository, historyRepository, null);
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

    @Transactional
    public ServiceProfitFollowUpPersistenceResult ensureSystem(
            UUID tenantId,
            UUID opportunityId,
            OffsetDateTime now
    ) {
        require(tenantId, "Tenant ID is required");
        require(opportunityId, "Opportunity ID is required");
        if (now == null) throw new IllegalArgumentException("Creation time is required");

        ServiceProfitOpportunity opportunity = opportunityRepository
                .findByIdAndTenantId(opportunityId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found in tenant"));
        var existingFollowUp = followUpRepository.findByTenantIdAndOpportunityId(tenantId, opportunityId);
        if (existingFollowUp.isPresent()) {
            return new ServiceProfitFollowUpPersistenceResult(existingFollowUp.get(), false);
        }

        if (systemCreationService == null) {
            ServiceProfitFollowUp followUp = followUpRepository.save(
                    ServiceProfitFollowUp.open(UUID.randomUUID(), opportunity.getTenantId(), opportunity.getId(), now)
            );
            historyRepository.save(ServiceProfitFollowUpHistory.recordSystem(
                    UUID.randomUUID(), tenantId, followUp.getId(), opportunityId,
                    ServiceProfitFollowUpEventType.CREATED, null,
                    ServiceProfitFollowUpHandlingStatus.OPEN.name(), 0, followUp.getVersion(), now
            ));
            return new ServiceProfitFollowUpPersistenceResult(followUp, true);
        }

        try {
            return new ServiceProfitFollowUpPersistenceResult(
                    systemCreationService.create(tenantId, opportunityId, now), true);
        } catch (DataIntegrityViolationException exception) {
            return followUpRepository.findByTenantIdAndOpportunityId(tenantId, opportunityId)
                    .map(followUp -> new ServiceProfitFollowUpPersistenceResult(followUp, false))
                    .orElseThrow(() -> exception);
        }
    }

    private void require(UUID value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }
}