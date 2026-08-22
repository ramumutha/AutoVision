package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import com.autovision.platform.serviceprofit.ServiceProfitPriority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EvidenceDerivedServiceProfitDetectionPolicy {

    public static final String POLICY_VERSION =
            "R1-EVIDENCE-DERIVED-1";

    private static final BigDecimal HIGH_THRESHOLD =
            new BigDecimal("10000.00");

    private static final BigDecimal MEDIUM_THRESHOLD =
            new BigDecimal("5000.00");

    private final ServiceProfitEvidencePhraseClassifier classifier;
    private final ServiceProfitOpportunityKeyFactory keyFactory;

    public EvidenceDerivedServiceProfitDetectionPolicy(
            ServiceProfitEvidencePhraseClassifier classifier,
            ServiceProfitOpportunityKeyFactory keyFactory
    ) {
        this.classifier = classifier;
        this.keyFactory = keyFactory;
    }

    public ServiceProfitDetectionResult detect(
            ServiceProfitDetectionInput input
    ) {
        if (input == null) {
            throw new IllegalArgumentException(
                    "Detection input is required"
            );
        }

        /*
         * Explicit source-confirmed states belong to R1.4.2 and must
         * not be reclassified as evidence-derived.
         */
        if (hasExplicitDisposition(input)) {
            return ServiceProfitDetectionResult.noMatch();
        }

        if (input.recommendationText() == null) {
            return ServiceProfitDetectionResult.noMatch();
        }

        ServiceProfitEvidenceSignal signal =
                classifier.classify(
                        input.advisorNote()
                );

        if (signal == ServiceProfitEvidenceSignal.NONE) {
            return ServiceProfitDetectionResult.noMatch();
        }

        ServiceProfitOpportunityType type =
                resolveType(signal);

        if (type == null) {
            return ServiceProfitDetectionResult.noMatch();
        }

        ServiceProfitEvidenceStrength strength =
                resolveStrength(signal);

        ServiceProfitActionability actionability =
                resolveActionability(
                        input,
                        signal
                );

        boolean suppressed =
                actionability
                        == ServiceProfitActionability.SUPPRESSED;

        return new ServiceProfitDetectionResult(
                true,
                suppressed,

                keyFactory.create(
                        input,
                        type
                ),

                type,
                ServiceProfitEvidenceClass.EVIDENCE_DERIVED,
                strength,
                resolvePriority(
                        input.recommendedAmount()
                ),
                actionability,

                titleFor(
                        type,
                        strength
                ),

                summaryFor(
                        input,
                        signal
                ),

                input.recommendedAmount(),
                input.currencyCode(),

                input.tenantId(),
                input.dealerId(),
                input.branchId(),
                input.locationId(),
                input.customerId(),
                input.vehicleId(),

                input.sourceSystem(),
                input.sourceEntityType(),
                input.sourceEntityId(),

                input.sourceServiceOrderId(),
                input.sourceServiceJobId(),
                input.sourceServiceLineId(),
                input.sourceQuoteId(),

                POLICY_VERSION
        );
    }

    private boolean hasExplicitDisposition(
            ServiceProfitDetectionInput input
    ) {
        return input.disposition()
                        == ServiceProfitDisposition.DECLINED
                || input.disposition()
                        == ServiceProfitDisposition.DEFERRED
                || input.recommendationStatus()
                        == ServiceProfitRecommendationStatus.DECLINED
                || input.recommendationStatus()
                        == ServiceProfitRecommendationStatus.DEFERRED;
    }

    private ServiceProfitOpportunityType resolveType(
            ServiceProfitEvidenceSignal signal
    ) {
        return switch (signal) {
            case STRONG_DECLINE ->
                    ServiceProfitOpportunityType.DECLINED_WORK;

            case STRONG_DEFER,
                 AMBIGUOUS_POSTPONEMENT,
                 AMBIGUOUS_IDENTITY ->
                    ServiceProfitOpportunityType.DECLINED_WORK;

            case COMPLETION,
                 NONE ->
                    null;
        };
    }

    private ServiceProfitEvidenceStrength resolveStrength(
            ServiceProfitEvidenceSignal signal
    ) {
        return switch (signal) {
            case STRONG_DECLINE,
                 STRONG_DEFER ->
                    ServiceProfitEvidenceStrength.STRONG;

            case AMBIGUOUS_POSTPONEMENT,
                 AMBIGUOUS_IDENTITY ->
                    ServiceProfitEvidenceStrength.MODERATE;

            case COMPLETION,
                 NONE ->
                    ServiceProfitEvidenceStrength.WEAK;
        };
    }

    private ServiceProfitActionability resolveActionability(
            ServiceProfitDetectionInput input,
            ServiceProfitEvidenceSignal signal
    ) {
        if (input.completedWorkEvidence()
                || signal
                == ServiceProfitEvidenceSignal.COMPLETION) {
            return ServiceProfitActionability.SUPPRESSED;
        }

        if (signal
                == ServiceProfitEvidenceSignal.AMBIGUOUS_IDENTITY) {
            return ServiceProfitActionability.REVIEW_REQUIRED;
        }

        if (!input.customerContactable()) {
            return ServiceProfitActionability.CONTACT_DATA_MISSING;
        }

        if (signal
                == ServiceProfitEvidenceSignal.AMBIGUOUS_POSTPONEMENT) {
            return ServiceProfitActionability.REVIEW_REQUIRED;
        }

        return ServiceProfitActionability.READY;
    }

    private ServiceProfitPriority resolvePriority(
            BigDecimal amount
    ) {
        if (amount == null) {
            return ServiceProfitPriority.LOW;
        }

        if (amount.compareTo(HIGH_THRESHOLD) >= 0) {
            return ServiceProfitPriority.HIGH;
        }

        if (amount.compareTo(MEDIUM_THRESHOLD) >= 0) {
            return ServiceProfitPriority.MEDIUM;
        }

        return ServiceProfitPriority.LOW;
    }

    private String titleFor(
            ServiceProfitOpportunityType type,
            ServiceProfitEvidenceStrength strength
    ) {
        if (strength
                == ServiceProfitEvidenceStrength.MODERATE) {
            return "Potential declined service opportunity";
        }

        return switch (type) {
            case DECLINED_WORK ->
                    "Evidence-derived declined service opportunity";

            case DEFERRED_WORK ->
                    "Evidence-derived deferred service opportunity";

            default ->
                    "Service opportunity";
        };
    }

    private String summaryFor(
            ServiceProfitDetectionInput input,
            ServiceProfitEvidenceSignal signal
    ) {
        if (input.advisorNote() != null) {
            return input.advisorNote();
        }

        return "Recommendation evidence produced signal: "
                + signal.name();
    }
}
