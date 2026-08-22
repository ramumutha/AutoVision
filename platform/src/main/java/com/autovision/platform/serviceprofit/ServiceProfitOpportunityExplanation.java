package com.autovision.platform.serviceprofit;

public record ServiceProfitOpportunityExplanation(
        String headline,
        String rationale,
        String evidenceBasis,
        String recommendedAction
) {
    public ServiceProfitOpportunityExplanation {
        requireText(headline, "Explanation headline is required");
        requireText(rationale, "Explanation rationale is required");
        requireText(evidenceBasis, "Explanation evidence basis is required");
        requireText(
                recommendedAction,
                "Explanation recommended action is required"
        );

        headline = headline.trim();
        rationale = rationale.trim();
        evidenceBasis = evidenceBasis.trim();
        recommendedAction = recommendedAction.trim();
    }

    private static void requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}