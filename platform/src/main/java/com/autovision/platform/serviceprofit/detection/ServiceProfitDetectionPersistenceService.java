package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunity;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityRepository;
import com.autovision.platform.serviceprofit.ServiceProfitSuppressionReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ServiceProfitDetectionPersistenceService {

    private final ServiceProfitOpportunityRepository repository;

    public ServiceProfitDetectionPersistenceService(
            ServiceProfitOpportunityRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public ServiceProfitPersistenceResult persist(
            ServiceProfitDetectionResult result,
            UUID principalId,
            OffsetDateTime detectedAt
    ) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "Detection result is required"
            );
        }

        if (!result.detected()) {
            return ServiceProfitPersistenceResult.noMatch();
        }

        if (result.tenantId() == null) {
            throw new IllegalArgumentException(
                    "Detected opportunity tenant ID is required"
            );
        }

        if (result.opportunityKey() == null
                || result.opportunityKey().isBlank()) {
            throw new IllegalArgumentException(
                    "Detected opportunity key is required"
            );
        }

        if (detectedAt == null) {
            throw new IllegalArgumentException(
                    "Detection persistence time is required"
            );
        }

        String opportunityKey =
                result.opportunityKey().trim();

        var existing =
                repository.findByTenantIdAndOpportunityKey(
                        result.tenantId(),
                        opportunityKey
                );

        if (existing.isPresent()) {
            return new ServiceProfitPersistenceResult(
                    ServiceProfitPersistenceOutcome.EXISTING,
                    existing.get()
            );
        }

        ServiceProfitOpportunity opportunity =
                ServiceProfitOpportunity.detect(
                        UUID.randomUUID(),
                        result.tenantId(),
                        result.dealerId(),
                        result.branchId(),
                        result.locationId(),
                        result.customerId(),
                        result.vehicleId(),
                        opportunityKey,
                        result.opportunityType(),
                        result.evidenceClass(),
                        result.evidenceStrength(),
                        result.priority(),
                        result.actionability(),
                        result.title(),
                        result.summary(),
                        result.potentialAmount(),
                        result.currencyCode(),
                        result.sourceSystem(),
                        result.sourceEntityType(),
                        result.sourceEntityId(),
                        result.sourceServiceOrderId(),
                        result.sourceServiceJobId(),
                        result.sourceServiceLineId(),
                        result.sourceQuoteId(),
                        result.policyVersion(),
                        principalId,
                        detectedAt
                );

        ServiceProfitPersistenceOutcome outcome =
                ServiceProfitPersistenceOutcome.CREATED;

        if (result.suppressed()) {
            opportunity.suppress(
                    ServiceProfitSuppressionReason.AUTHORITATIVE_COMPLETION_EVIDENCE,
                    principalId,
                    detectedAt
            );

            outcome =
                    ServiceProfitPersistenceOutcome.CREATED_SUPPRESSED;
        }

        ServiceProfitOpportunity persisted =
                repository.save(opportunity);

        return new ServiceProfitPersistenceResult(
                outcome,
                persisted
        );
    }
}