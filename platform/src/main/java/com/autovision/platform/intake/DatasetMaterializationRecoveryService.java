package com.autovision.platform.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DatasetMaterializationRecoveryService {
    private final DatasetProcessingService datasetService;
    private final DatasetOperationalEvidenceService evidenceService;

    public DatasetMaterializationRecoveryService(DatasetProcessingService datasetService,
            DatasetOperationalEvidenceService evidenceService) {
        this.datasetService = datasetService;
        this.evidenceService = evidenceService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DatasetProcessing markAbandonedMaterializationFailedForRecovery(UUID tenantId, UUID processingId,
            UUID principalId, String applicationId, String recoveryCode, String safeReason, boolean replayable,
            OffsetDateTime now) {
        if (principalId == null && (applicationId == null || applicationId.isBlank())) {
            throw new IllegalArgumentException("Recovery actor or application identity is required");
        }
        if (recoveryCode == null || recoveryCode.isBlank() || safeReason == null || safeReason.isBlank()) {
            throw new IllegalArgumentException("Recovery code and safe reason are required");
        }
        DatasetProcessing processing = datasetService.requireDataset(tenantId, processingId);
        processing.markAbandonedMaterializationFailed(safeReason, MaterializationFailureStage.LIFECYCLE,
                MaterializationFailureCode.PERSISTENCE_FAILED, replayable, now);
        DatasetProcessing saved = datasetService.receiveDataset(processing);
        evidenceService.updateLifecycleSummary(tenantId, saved, now);
        evidenceService.recordEvent(tenantId, DatasetOperationalEvent.record(UUID.randomUUID(), saved,
            DatasetOperationalEventType.ABANDONED_MATERIALIZATION_MARKED_FAILED,
            saved.getMaterializationKey(), saved.getMaterializationVersion(),
            saved.getMaterializationAttemptCount(), principalId, applicationId,
            recoveryCode.trim(), safeReason.trim(), now));
        return saved;
    }
}