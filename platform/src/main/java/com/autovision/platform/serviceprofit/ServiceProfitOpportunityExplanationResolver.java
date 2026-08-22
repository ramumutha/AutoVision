package com.autovision.platform.serviceprofit;

public final class ServiceProfitOpportunityExplanationResolver {

    private ServiceProfitOpportunityExplanationResolver() {
    }

    public static ServiceProfitOpportunityExplanation resolve(
            ServiceProfitOpportunity opportunity
    ) {
        if (opportunity == null) {
            throw new IllegalArgumentException(
                    "Service profit opportunity is required"
            );
        }

        return new ServiceProfitOpportunityExplanation(
                resolveHeadline(opportunity),
                resolveRationale(opportunity),
                resolveEvidenceBasis(opportunity),
                resolveRecommendedAction(opportunity)
        );
    }

    private static String resolveHeadline(
            ServiceProfitOpportunity opportunity
    ) {
        return switch (opportunity.getOpportunityType()) {
            case DECLINED_WORK ->
                    "Previously declined work may be recoverable";
            case DEFERRED_WORK ->
                    "Deferred work may now be ready for follow-up";
            case DUE_SERVICE ->
                    "Scheduled service is due";
            case OVERDUE_SERVICE ->
                    "Scheduled service appears overdue";
            case INACTIVE_CUSTOMER ->
                    "Customer service activity appears inactive";
        };
    }

    private static String resolveRationale(
            ServiceProfitOpportunity opportunity
    ) {
        String base = switch (opportunity.getOpportunityType()) {
            case DECLINED_WORK ->
                    "The available service history indicates work that was declined and may remain outstanding.";
            case DEFERRED_WORK ->
                    "The available service history indicates work that was deferred for later consideration.";
            case DUE_SERVICE ->
                    "The available service data indicates that a scheduled service threshold has been reached.";
            case OVERDUE_SERVICE ->
                    "The available service data indicates that a scheduled service threshold has passed.";
            case INACTIVE_CUSTOMER ->
                    "The available customer activity data indicates an extended period without recorded service activity.";
        };

        if (opportunity.getStatus()
                == ServiceProfitOpportunityStatus.SUPPRESSED) {
            return base
                    + " The opportunity is currently suppressed because authoritative information indicates that it should not be actioned.";
        }

        return base;
    }

    private static String resolveEvidenceBasis(
            ServiceProfitOpportunity opportunity
    ) {
        String evidenceClass =
                switch (opportunity.getEvidenceClass()) {
                    case SOURCE_CONFIRMED ->
                            "The opportunity is supported directly by an authoritative dealer source record.";
                    case EVIDENCE_DERIVED ->
                            "The opportunity is inferred from available service evidence rather than an explicit source disposition.";
                    case POLICY_DERIVED ->
                            "The opportunity is identified by applying the configured Service Profit lifecycle policy to available dealer data.";
                };

        String strength =
                switch (opportunity.getEvidenceStrength()) {
                    case STRONG ->
                            " Evidence strength is strong.";
                    case MODERATE ->
                            " Evidence strength is moderate.";
                    case WEAK ->
                            " Evidence strength is weak.";
                };

        return evidenceClass + strength;
    }

    private static String resolveRecommendedAction(
            ServiceProfitOpportunity opportunity
    ) {
        return switch (opportunity.getActionability()) {
            case READY ->
                    "Review the opportunity and proceed with the appropriate customer follow-up.";
            case REVIEW_REQUIRED ->
                    "Review the supporting source information before contacting the customer.";
            case CONTACT_DATA_MISSING ->
                    "Resolve the missing customer contact information before attempting follow-up.";
            case BLOCKED ->
                    "Resolve the blocking condition before taking customer-facing action.";
            case SUPPRESSED ->
                    resolveSuppressedAction(opportunity);
        };
    }

    private static String resolveSuppressedAction(
            ServiceProfitOpportunity opportunity
    ) {
        if (opportunity.getSuppressionReason() == null) {
            return "No customer-facing action should be taken while this opportunity is suppressed.";
        }

        return switch (opportunity.getSuppressionReason()) {
            case WORK_ALREADY_COMPLETED ->
                    "No follow-up is required because the work is already recorded as completed.";
            case ALREADY_INVOICED ->
                    "No follow-up is required because the work is already represented by invoice evidence.";
            case AUTHORITATIVE_COMPLETION_EVIDENCE ->
                    "No follow-up is required because authoritative completion evidence supersedes the opportunity.";
            case DUPLICATE_OPPORTUNITY ->
                    "Do not action this record because it represents a duplicate opportunity.";
        };
    }
}