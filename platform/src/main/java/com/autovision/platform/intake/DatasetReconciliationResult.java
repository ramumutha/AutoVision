package com.autovision.platform.intake;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DatasetReconciliationResult(
        UUID datasetProcessingId,
        UUID tenantId,
        String datasetId,
        String datasetVersion,
        DatasetProcessingStatus status,
        int recordsReceived,
        int recordsParsed,
        int recordsStaged,
        int recordsQuarantined,
        int recordsExcluded,
        int recordsEligible,
        int recordsMapped,
        int opportunitiesDetected,
        int opportunitiesPersisted,
        int duplicateNoOpCount,
        FindingSummary findings,
        int materializationAttempt,
        String materializationKey,
        String materializationVersion,
        OffsetDateTime materializationStartedAt,
        OffsetDateTime materializedAt,
        OffsetDateTime materializationFailedAt,
        MaterializationFailureStage failureStage,
        MaterializationFailureCode failureCode,
        Boolean replayable,
        boolean consistent
) { }