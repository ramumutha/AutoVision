package com.autovision.platform.intake;

import java.util.UUID;

public record ControlledDatasetProcessingResult(
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
}