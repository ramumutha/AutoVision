package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunity;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityEvidenceService;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityRepository;
import com.autovision.platform.serviceprofit.ServiceProfitSuppressionReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ServiceProfitDetectionPersistenceService {

    private final ServiceProfitOpportunityRepository repository;
        private final ServiceProfitOpportunityEvidenceService evidenceService;

        @Autowired
    public ServiceProfitDetectionPersistenceService(
                        ServiceProfitOpportunityRepository repository,
                        ServiceProfitOpportunityEvidenceService evidenceService
    ) {
        this.repository = repository;
                this.evidenceService = evidenceService;
        }

        public ServiceProfitDetectionPersistenceService(
                        ServiceProfitOpportunityRepository repository
        ) {
                this(repository, null);
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
                        persistEvidence(result, existing.get(), detectedAt);
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

        persistEvidence(result, persisted, detectedAt);

        return new ServiceProfitPersistenceResult(
                outcome,
                persisted
        );
    }

        private void persistEvidence(
                        ServiceProfitDetectionResult result,
                        ServiceProfitOpportunity opportunity,
                        OffsetDateTime capturedAt
        ) {
                if (evidenceService != null && !result.evidence().isEmpty()) {
                        evidenceService.persist(
                                        opportunity,
                                        result.evidence(),
                                        result.sourceSystem(),
                                        result.evidenceClass(),
                                        result.evidenceStrength(),
                                        capturedAt
                        );
                }
        }
}