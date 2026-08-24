package com.autovision.platform.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ControlledDatasetMaterializationFailureService {

    private final DatasetProcessingService datasetService;

    public ControlledDatasetMaterializationFailureService(DatasetProcessingService datasetService) {
        this.datasetService = datasetService;
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
        datasetService.markMaterializationFailed(tenantId, datasetProcessingId, materializationKey,
                materializationVersion, failureReason, failedAt);
    }
}
