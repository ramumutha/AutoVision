package com.autovision.platform.serviceprofit;

import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceRef;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ServiceProfitOpportunityEvidenceService {

    private final ServiceProfitOpportunityEvidenceRepository repository;

    public ServiceProfitOpportunityEvidenceService(
            ServiceProfitOpportunityEvidenceRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public void persist(
            ServiceProfitOpportunity opportunity,
            Iterable<ServiceProfitEvidenceRef> references,
            String sourceSystem,
            ServiceProfitEvidenceClass evidenceClassification,
            ServiceProfitEvidenceStrength evidenceStrength,
            OffsetDateTime capturedAt
    ) {
        if (opportunity == null || opportunity.getId() == null || opportunity.getTenantId() == null) {
            throw new IllegalArgumentException("Service Profit opportunity is required");
        }
        if (references == null) {
            return;
        }

        for (ServiceProfitEvidenceRef reference : references) {
            if (!repository.existsByTenantIdAndOpportunityIdAndEvidenceSourceTypeAndSourceSystemAndSourceRecordId(
                    opportunity.getTenantId(),
                    opportunity.getId(),
                    reference.sourceType(),
                    sourceSystem,
                    reference.sourceId()
            )) {
                repository.save(ServiceProfitOpportunityEvidence.capture(
                        UUID.randomUUID(),
                        opportunity,
                        reference,
                        sourceSystem,
                        evidenceClassification,
                        evidenceStrength,
                        capturedAt
                ));
            }
        }
    }
}