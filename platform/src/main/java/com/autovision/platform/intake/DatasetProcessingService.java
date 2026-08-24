package com.autovision.platform.intake;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DatasetProcessingService {

    private final DatasetProcessingRepository datasetRepository;
    private final StagedSourceRecordRepository recordRepository;
    private final ValidationFindingRepository findingRepository;

    public DatasetProcessingService(
            DatasetProcessingRepository datasetRepository,
            StagedSourceRecordRepository recordRepository,
            ValidationFindingRepository findingRepository
    ) {
        this.datasetRepository = datasetRepository;
        this.recordRepository = recordRepository;
        this.findingRepository = findingRepository;
    }

    @Transactional
    public DatasetProcessing receiveDataset(
            DatasetProcessing processing
    ) {
        return datasetRepository.save(processing);
    }

    @Transactional
    public StagedSourceRecord stageRecord(
            UUID tenantId,
            UUID datasetProcessingId,
            StagedRecordType recordType,
            String sourceRecordId,
            String sourceParentId,
            String sourceVersion,
            String sourceHash,
            tools.jackson.databind.JsonNode rawPayload,
            OffsetDateTime now
    ) {
        DatasetProcessing processing = requireDataset(
                tenantId,
                datasetProcessingId
        );
        StagedSourceRecord record = StagedSourceRecord.stage(
                UUID.randomUUID(),
                tenantId,
                datasetProcessingId,
                recordType,
                sourceRecordId,
                sourceParentId,
                sourceVersion,
                sourceHash,
                rawPayload,
                now
        );
        StagedSourceRecord saved = recordRepository.save(record);
        processing.markStaged(now);
        datasetRepository.save(processing);
        return saved;
    }

    @Transactional
    public ValidationFinding attachFinding(
            UUID tenantId,
            UUID datasetProcessingId,
            UUID stagedSourceRecordId,
            ValidationStage validationStage,
            ValidationSeverity severity,
            String findingCode,
            String fieldPath,
            String safeMessage,
            String capabilityAffected,
            OffsetDateTime now
    ) {
        DatasetProcessing processing = requireDataset(
                tenantId,
                datasetProcessingId
        );
        if (stagedSourceRecordId != null) {
            StagedSourceRecord record = recordRepository
                    .findByIdAndTenantId(stagedSourceRecordId, tenantId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Staged source record was not found"
                    ));
            if (!datasetProcessingId.equals(record.getDatasetProcessingId())) {
                throw new IllegalArgumentException(
                        "Staged source record does not belong to dataset"
                );
            }
        }
        ValidationFinding finding = ValidationFinding.record(
                UUID.randomUUID(),
                tenantId,
                processing.getId(),
                stagedSourceRecordId,
                validationStage,
                severity,
                findingCode,
                fieldPath,
                safeMessage,
                capabilityAffected,
                now
        );
        return findingRepository.save(finding);
    }

        @Transactional
        public StagedSourceRecord markRecordPassed(
                        UUID tenantId,
                        UUID datasetProcessingId,
                        UUID stagedSourceRecordId
        ) {
                requireDataset(tenantId, datasetProcessingId);
                StagedSourceRecord record = recordRepository
                                .findByIdAndTenantId(stagedSourceRecordId, tenantId)
                                .orElseThrow(() -> new EntityNotFoundException("Staged source record was not found"));
                if (!datasetProcessingId.equals(record.getDatasetProcessingId())) {
                        throw new IllegalArgumentException("Staged source record does not belong to dataset");
                }
                record.markValidationPassed();
                return recordRepository.save(record);
        }

        @Transactional
        public DatasetProcessing markValidationFailed(
                        UUID tenantId,
                        UUID datasetProcessingId,
                        OffsetDateTime now
        ) {
                DatasetProcessing processing = requireDataset(tenantId, datasetProcessingId);
                processing.markValidationFailed(now);
                return datasetRepository.save(processing);
        }

    @Transactional
    public DatasetProcessing markReadyForMaterialization(
            UUID tenantId,
            UUID datasetProcessingId,
            OffsetDateTime now
    ) {
        DatasetProcessing processing = requireDataset(tenantId, datasetProcessingId);
        List<StagedSourceRecord> records = recordsFor(tenantId, datasetProcessingId);
        if (records.stream().anyMatch(record -> record.getState() == StagedRecordState.STAGED
                && (record.getValidationStatus() != StagedRecordValidationStatus.PASSED
                || !record.isMaterializationEligible()))) {
            throw new IllegalStateException("Dataset contains records that are not eligible for materialization");
        }
        processing.markReadyForMaterialization(now);
        return datasetRepository.save(processing);
    }

    @Transactional
    public DatasetProcessing beginMaterialization(
            UUID tenantId,
            UUID datasetProcessingId,
            String materializationKey,
            String materializationVersion,
            UUID correlationId,
            OffsetDateTime now
    ) {
        DatasetProcessing processing = requireDataset(tenantId, datasetProcessingId);
        processing.beginMaterialization(materializationKey, materializationVersion, correlationId, now);
        return datasetRepository.save(processing);
    }

    @Transactional
    public DatasetProcessing markMaterialized(
            UUID tenantId,
            UUID datasetProcessingId,
            String materializationKey,
            String materializationVersion,
            OffsetDateTime now
    ) {
        DatasetProcessing processing = requireDataset(tenantId, datasetProcessingId);
        processing.markMaterialized(materializationKey, materializationVersion, now);
        return datasetRepository.save(processing);
    }

    @Transactional
    public DatasetProcessing markMaterializationFailed(
            UUID tenantId,
            UUID datasetProcessingId,
            String materializationKey,
            String materializationVersion,
            String failureReason,
            OffsetDateTime now
    ) {
        DatasetProcessing processing = requireDataset(tenantId, datasetProcessingId);
        processing.markMaterializationFailed(
                materializationKey, materializationVersion, failureReason, now);
        return datasetRepository.save(processing);
    }

    @Transactional
    public StagedSourceRecord quarantineRecord(
            UUID tenantId,
            UUID datasetProcessingId,
            UUID stagedSourceRecordId,
            OffsetDateTime now
    ) {
        DatasetProcessing processing = requireDataset(
                tenantId,
                datasetProcessingId
        );
        StagedSourceRecord record = recordRepository
                .findByIdAndTenantId(stagedSourceRecordId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Staged source record was not found"
                ));
        if (!datasetProcessingId.equals(record.getDatasetProcessingId())) {
            throw new IllegalArgumentException(
                    "Staged source record does not belong to dataset"
            );
        }
        record.markValidationFailed();
        record.quarantine();
        processing.quarantine(now);
        datasetRepository.save(processing);
        return recordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public DatasetProcessing requireDataset(UUID tenantId, UUID datasetProcessingId) {
        return datasetRepository
                .findByIdAndTenantId(datasetProcessingId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Dataset processing was not found"
                ));
    }

    @Transactional(readOnly = true)
    public List<StagedSourceRecord> recordsFor(
            UUID tenantId,
            UUID datasetProcessingId
    ) {
        requireDataset(tenantId, datasetProcessingId);
        return recordRepository
                .findByTenantIdAndDatasetProcessingIdOrderByCreatedAtAsc(
                        tenantId,
                        datasetProcessingId
                );
    }

    @Transactional(readOnly = true)
    public List<ValidationFinding> findingsFor(
            UUID tenantId,
            UUID datasetProcessingId
    ) {
        requireDataset(tenantId, datasetProcessingId);
        return findingRepository
                .findByTenantIdAndDatasetProcessingIdOrderByCreatedAtAsc(
                        tenantId,
                        datasetProcessingId
                );
    }
}