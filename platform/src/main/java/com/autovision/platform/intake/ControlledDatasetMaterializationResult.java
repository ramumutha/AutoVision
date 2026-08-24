package com.autovision.platform.intake;

import com.autovision.platform.intake.DatasetProcessingStatus;

public record ControlledDatasetMaterializationResult(
        DatasetProcessingStatus status,
        int mappedRecords,
        int detectedOpportunities,
        int createdOpportunities,
        int existingOpportunities,
        boolean duplicate
) {
}
