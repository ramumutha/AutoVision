package com.autovision.platform.intake;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StagedSourceRecordRepository
        extends JpaRepository<StagedSourceRecord, UUID> {

    Optional<StagedSourceRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<StagedSourceRecord>
    findByTenantIdAndDatasetProcessingIdAndRecordTypeAndSourceRecordId(
            UUID tenantId,
            UUID datasetProcessingId,
            StagedRecordType recordType,
            String sourceRecordId
    );

    List<StagedSourceRecord> findByTenantIdAndDatasetProcessingIdOrderByCreatedAtAsc(
            UUID tenantId,
            UUID datasetProcessingId
    );
}