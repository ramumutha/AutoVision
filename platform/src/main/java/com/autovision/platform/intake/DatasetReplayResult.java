package com.autovision.platform.intake;

import java.util.UUID;

public record DatasetReplayResult(
        UUID datasetProcessingId,
        DatasetProcessingStatus status,
        int attempt,
        boolean noOp,
        boolean replayable
) { }