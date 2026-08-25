package com.autovision.platform.serviceprofit;

import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceProfitOpportunityEvidenceRepository
        extends JpaRepository<ServiceProfitOpportunityEvidence, UUID> {

    List<ServiceProfitOpportunityEvidence> findAllByTenantIdAndOpportunityId(
            UUID tenantId,
            UUID opportunityId
    );

    boolean existsByTenantIdAndOpportunityIdAndEvidenceSourceTypeAndSourceSystemAndSourceRecordId(
            UUID tenantId,
            UUID opportunityId,
            ServiceProfitEvidenceSourceType evidenceSourceType,
            String sourceSystem,
            String sourceRecordId
    );
}