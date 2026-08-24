package com.autovision.platform.intake;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatasetOperationalEventRepository extends JpaRepository<DatasetOperationalEvent, UUID> {
    List<DatasetOperationalEvent> findByTenantIdAndDatasetProcessingIdOrderByOccurredAtAscId(UUID tenantId, UUID datasetProcessingId);
}