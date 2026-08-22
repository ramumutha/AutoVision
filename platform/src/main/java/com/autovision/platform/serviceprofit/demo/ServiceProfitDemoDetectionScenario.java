package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionInput;

public record ServiceProfitDemoDetectionScenario(
        String scenarioId,
        ServiceProfitDetectionInput input
) {
    public ServiceProfitDemoDetectionScenario {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException(
                    "Demo detection scenario ID is required"
            );
        }

        if (input == null) {
            throw new IllegalArgumentException(
                    "Demo detection input is required"
            );
        }

        scenarioId = scenarioId.trim();
    }
}