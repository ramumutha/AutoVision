package com.autovision.platform.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ControlledDatasetMaterializationFailureService {

    private final DatasetProcessingService datasetService;
    private final DatasetOperationalEvidenceService evidenceService;

    public ControlledDatasetMaterializationFailureService(DatasetProcessingService datasetService,
            DatasetOperationalEvidenceService evidenceService) {
        this.datasetService = datasetService;
        this.evidenceService = evidenceService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID tenantId,
            UUID datasetProcessingId,
            String materializationKey,
            String materializationVersion,
            String failureReason,
            OffsetDateTime failedAt
    ) {
            record(tenantId, datasetProcessingId, materializationKey, materializationVersion, failureReason,
                MaterializationFailureStage.UNKNOWN, MaterializationFailureCode.UNKNOWN, true, failedAt);
            }

            @Transactional(propagation = Propagation.REQUIRES_NEW)
            public void record(
                UUID tenantId,
                UUID datasetProcessingId,
                String materializationKey,
                String materializationVersion,
                String failureReason,
                MaterializationFailureStage failureStage,
                MaterializationFailureCode failureCode,
                boolean replayable,
                OffsetDateTime failedAt
            ) {
            DatasetProcessing processing = datasetService.requireDataset(tenantId, datasetProcessingId);
            processing.recordMaterializationFailure(materializationKey, materializationVersion, failureReason,
                failureStage, failureCode, replayable, failedAt);
            datasetService.receiveDataset(processing);
            evidenceService.updateLifecycleSummary(tenantId, processing, failedAt);
    }
}
