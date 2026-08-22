package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.detection.ServiceProfitPersistenceOutcome;

import java.util.List;

public record ServiceProfitDemoMaterializationResult(
        int totalScenariosEvaluated,
        long createdOpportunities,
        long createdSuppressedOpportunities,
        long existingOpportunities,
        long noMatchScenarios,
        List<ScenarioOutcome> scenarioOutcomes
) {

    public ServiceProfitDemoMaterializationResult {
        scenarioOutcomes = List.copyOf(scenarioOutcomes);
    }

    public static ServiceProfitDemoMaterializationResult from(
            List<ScenarioOutcome> scenarioOutcomes
    ) {
        List<ScenarioOutcome> outcomes =
                List.copyOf(scenarioOutcomes);

        return new ServiceProfitDemoMaterializationResult(
                outcomes.size(),
                count(outcomes, ServiceProfitPersistenceOutcome.CREATED),
                count(
                        outcomes,
                        ServiceProfitPersistenceOutcome.CREATED_SUPPRESSED
                ),
                count(outcomes, ServiceProfitPersistenceOutcome.EXISTING),
                count(outcomes, ServiceProfitPersistenceOutcome.NO_MATCH),
                outcomes
        );
    }

    private static long count(
            List<ScenarioOutcome> outcomes,
            ServiceProfitPersistenceOutcome expected
    ) {
        return outcomes.stream()
                .filter(outcome -> outcome.outcome() == expected)
                .count();
    }

    public record ScenarioOutcome(
            String scenarioId,
            ServiceProfitPersistenceOutcome outcome
    ) {
        public ScenarioOutcome {
            if (scenarioId == null || scenarioId.isBlank()) {
                throw new IllegalArgumentException(
                        "Demo scenario ID is required"
                );
            }

            if (outcome == null) {
                throw new IllegalArgumentException(
                        "Demo scenario persistence outcome is required"
                );
            }

            scenarioId = scenarioId.trim();
        }
    }
}