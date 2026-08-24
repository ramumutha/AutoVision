package com.autovision.platform.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DatasetReplayService {
    private final DatasetProcessingService datasetService;
    private final ControlledDatasetMaterializer materializer;
    private final DatasetOperationalEvidenceService evidenceService;

    public DatasetReplayService(DatasetProcessingService datasetService, ControlledDatasetMaterializer materializer,
            DatasetOperationalEvidenceService evidenceService) {
        this.datasetService = datasetService;
        this.materializer = materializer;
        this.evidenceService = evidenceService;
    }

    @Transactional
    public DatasetReplayResult replay(UUID tenantId, UUID processingId, UUID principalId, String applicationId,
            OffsetDateTime now) {
        DatasetProcessing current = datasetService.requireDataset(tenantId, processingId);
        return replay(tenantId, processingId, principalId, applicationId, current.getMaterializationKey(),
            current.getMaterializationVersion(), now);
        }

        @Transactional
        public DatasetReplayResult replay(UUID tenantId, UUID processingId, UUID principalId, String applicationId,
            String materializationKey, String materializationVersion, OffsetDateTime now) {
        requireActor(principalId, applicationId);
        DatasetProcessing processing = datasetService.requireDataset(tenantId, processingId);
        if (processing.getStatus() == DatasetProcessingStatus.MATERIALIZED) {
            if (!sameIdentity(processing, materializationKey, materializationVersion)) {
            throw new IllegalStateException("Materialization identity does not match dataset");
            }
            return new DatasetReplayResult(processingId, processing.getStatus(), processing.getMaterializationAttemptCount(), true, false);
        }
        if (processing.getStatus() != DatasetProcessingStatus.MATERIALIZATION_FAILED
                || !Boolean.TRUE.equals(processing.getMaterializationFailureReplayable())) {
            throw new IllegalStateException("Dataset is not eligible for controlled replay");
        }
        recordEvent(tenantId, processing, DatasetOperationalEventType.REPLAY_REQUESTED, principalId, applicationId,
                "REPLAY_REQUESTED", "Controlled replay requested", now);
        recordEvent(tenantId, processing, DatasetOperationalEventType.REPLAY_STARTED, principalId, applicationId,
                "REPLAY_STARTED", "Controlled replay started", now);
        try {
            materializer.materialize(tenantId, processingId, principalId, now);
            DatasetProcessing completed = datasetService.requireDataset(tenantId, processingId);
            recordEvent(tenantId, completed, DatasetOperationalEventType.REPLAY_SUCCEEDED, principalId, applicationId,
                    "REPLAY_SUCCEEDED", "Controlled replay completed", now);
            return new DatasetReplayResult(processingId, completed.getStatus(), completed.getMaterializationAttemptCount(), false, false);
        } catch (RuntimeException exception) {
            DatasetProcessing failed = datasetService.requireDataset(tenantId, processingId);
            recordEvent(tenantId, failed, DatasetOperationalEventType.REPLAY_FAILED, principalId, applicationId,
                    "REPLAY_FAILED", "Controlled replay failed", now);
            throw exception;
        }
    }

    private boolean sameIdentity(DatasetProcessing processing, String key, String version) {
        return key != null && version != null && key.equals(processing.getMaterializationKey())
                && version.equals(processing.getMaterializationVersion());
    }

    private void recordEvent(UUID tenantId, DatasetProcessing processing, DatasetOperationalEventType type,
            UUID principalId, String applicationId, String code, String reason, OffsetDateTime now) {
        evidenceService.recordEvent(tenantId, DatasetOperationalEvent.record(UUID.randomUUID(), processing, type,
                processing.getMaterializationKey(), processing.getMaterializationVersion(),
                processing.getMaterializationAttemptCount(), principalId, applicationId, code, reason, now));
    }

    private static void requireActor(UUID principalId, String applicationId) {
        if (principalId == null && (applicationId == null || applicationId.isBlank())) {
            throw new IllegalArgumentException("Replay actor or application identity is required");
        }
        if (principalId == null) throw new IllegalArgumentException("Replay execution requires an authorized principal");
    }
}