package com.autovision.platform.serviceprofit.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DealerDataAssessmentRepository
        extends JpaRepository<DealerDataAssessment, UUID> {

    Optional<DealerDataAssessment> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    List<DealerDataAssessment>
    findByTenantIdOrderByCreatedAtDesc(
            UUID tenantId
    );

    List<DealerDataAssessment>
    findByTenantIdAndDealerIdOrderByCreatedAtDesc(
            UUID tenantId,
            UUID dealerId
    );

    Optional<DealerDataAssessment>
    findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
            UUID tenantId,
            String sourceDatasetId,
            String sourceDatasetVersion
    );
    Optional<DealerDataAssessment> findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId,
            DealerDataAssessmentStatus status
    );
}
