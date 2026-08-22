package com.autovision.platform.serviceprofit.detection;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ServiceProfitDetectionOrchestrator {

    private final ExplicitServiceProfitDetectionPolicy explicitPolicy;
    private final EvidenceDerivedServiceProfitDetectionPolicy evidencePolicy;
    private final LifecycleServiceProfitDetectionPolicy lifecyclePolicy;

    public ServiceProfitDetectionOrchestrator(
            ExplicitServiceProfitDetectionPolicy explicitPolicy,
            EvidenceDerivedServiceProfitDetectionPolicy evidencePolicy,
            LifecycleServiceProfitDetectionPolicy lifecyclePolicy
    ) {
        this.explicitPolicy = explicitPolicy;
        this.evidencePolicy = evidencePolicy;
        this.lifecyclePolicy = lifecyclePolicy;
    }

    public ServiceProfitDetectionResult detect(
            ServiceProfitDetectionInput input,
            OffsetDateTime evaluatedAt
    ) {
        if (input == null) {
            throw new IllegalArgumentException(
                    "Detection input is required"
            );
        }

        if (evaluatedAt == null) {
            throw new IllegalArgumentException(
                    "Detection evaluation time is required"
            );
        }

        ServiceProfitDetectionResult explicit =
                explicitPolicy.detect(input);

        if (explicit.detected()) {
            return explicit;
        }

        ServiceProfitDetectionResult evidenceDerived =
                evidencePolicy.detect(input);

        if (evidenceDerived.detected()) {
            return evidenceDerived;
        }

        ServiceProfitDetectionResult lifecycle =
                lifecyclePolicy.detect(
                        input,
                        evaluatedAt
                );

        if (lifecycle.detected()) {
            return lifecycle;
        }

        return ServiceProfitDetectionResult.noMatch();
    }
}