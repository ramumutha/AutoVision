package com.autovision.platform.intake;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetProcessingRepository
        extends JpaRepository<DatasetProcessing, UUID> {

    Optional<DatasetProcessing> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<DatasetProcessing> findByTenantIdAndDatasetIdAndDatasetVersion(
            UUID tenantId,
            String datasetId,
            String datasetVersion
    );

    List<DatasetProcessing> findByTenantIdOrderByReceivedAtDesc(UUID tenantId);
}