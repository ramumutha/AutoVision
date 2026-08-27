package com.autovision.platform.serviceprofit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
/** Activates only dealer-ready opportunities as internal, unassigned work. */
public class ServiceProfitFollowUpActivationService {

    private final ServiceProfitFollowUpPersistenceService persistenceService;

    public ServiceProfitFollowUpActivationService(
            ServiceProfitFollowUpPersistenceService persistenceService
    ) {
        this.persistenceService = persistenceService;
    }

    @Transactional
    public ServiceProfitFollowUpActivationResult activate(
            ServiceProfitOpportunity opportunity,
            OffsetDateTime now
    ) {
        if (opportunity == null) throw new IllegalArgumentException("Opportunity is required");
        if (now == null) throw new IllegalArgumentException("Activation time is required");

        if (!eligible(opportunity)) return null;

        ServiceProfitFollowUpPersistenceResult result = persistenceService.ensureSystem(
                opportunity.getTenantId(), opportunity.getId(), now);
        ServiceProfitFollowUp followUp = result.followUp();
        return new ServiceProfitFollowUpActivationResult(
                opportunity.getId(), followUp.getId(), result.created(),
                followUp.getHandlingStatus(), followUp.getOwnerPrincipalId(),
                followUp.getCurrentDisposition(), followUp.getNextActionDueAt());
    }

    private boolean eligible(ServiceProfitOpportunity opportunity) {
        if (opportunity.getActionability() != ServiceProfitActionability.READY) return false;
        return switch (opportunity.getStatus()) {
            case DETECTED, QUALIFIED, ASSIGNED, CONTACTED -> true;
            case CLOSED, REJECTED, INVALID, DUPLICATE, SUPPRESSED, EXPIRED -> false;
        };
    }
}