package com.autovision.platform.intake;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ValidationFindingRepository
        extends JpaRepository<ValidationFinding, UUID> {

    List<ValidationFinding> findByTenantIdAndDatasetProcessingIdOrderByCreatedAtAsc(
            UUID tenantId,
            UUID datasetProcessingId
    );

    List<ValidationFinding> findByTenantIdAndStagedSourceRecordIdOrderByCreatedAtAsc(
            UUID tenantId,
            UUID stagedSourceRecordId
    );
}