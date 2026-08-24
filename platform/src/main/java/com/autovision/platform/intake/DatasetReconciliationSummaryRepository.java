package com.autovision.platform.intake;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DatasetReconciliationSummaryRepository extends JpaRepository<DatasetReconciliationSummary, UUID> {
    Optional<DatasetReconciliationSummary> findByTenantIdAndDatasetProcessingId(UUID tenantId, UUID datasetProcessingId);
}