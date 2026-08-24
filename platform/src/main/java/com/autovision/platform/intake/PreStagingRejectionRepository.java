package com.autovision.platform.intake;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PreStagingRejectionRepository extends JpaRepository<PreStagingRejection, UUID> {
    List<PreStagingRejection> findByTenantIdAndDatasetProcessingIdOrderByOccurredAtAscId(UUID tenantId, UUID datasetProcessingId);
}