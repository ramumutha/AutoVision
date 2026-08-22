package com.autovision.platform.serviceprofit.demo;

import java.util.UUID;

public record ServiceProfitDemoLoadResult(
        boolean loaded,
        boolean alreadyPresent,
        UUID assessmentId,
        String datasetId,
        String datasetVersion
) {
}
