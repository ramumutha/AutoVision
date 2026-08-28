package com.autovision.platform.intake;

import java.util.UUID;

public record ControlledDatasetIntakeResponse(
        UUID datasetProcessingId,
        String datasetId,
        String datasetVersion,
        DatasetProcessingStatus status,
        int recordsReceived,
        int recordsStaged,
        int recordsQuarantined,
        int fatalCount,
        int errorCount,
        int warningCount,
        int infoCount,
        boolean duplicateNoOp
) {
    public static ControlledDatasetIntakeResponse from(ControlledDatasetProcessingResult result) {
        return new ControlledDatasetIntakeResponse(result.datasetProcessingId(), result.datasetId(),
                result.datasetVersion(), result.status(), result.recordsReceived(), result.recordsStaged(),
                result.recordsQuarantined(), result.fatalCount(), result.errorCount(), result.warningCount(),
                result.infoCount(), result.duplicateNoOp());
    }
}