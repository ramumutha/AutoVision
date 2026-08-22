package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import com.autovision.platform.serviceprofit.ServiceProfitPriority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExplicitServiceProfitDetectionPolicy {

    public static final String POLICY_VERSION =
            "R1-EXPLICIT-DETECTION-1";

    private static final BigDecimal HIGH_THRESHOLD =
            new BigDecimal("10000.00");

    private static final BigDecimal MEDIUM_THRESHOLD =
            new BigDecimal("5000.00");

    private final ServiceProfitOpportunityKeyFactory keyFactory;

    public ExplicitServiceProfitDetectionPolicy(
            ServiceProfitOpportunityKeyFactory keyFactory
    ) {
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

        ServiceProfitOpportunityType opportunityType =
                resolveOpportunityType(input);

        if (opportunityType == null) {
            return ServiceProfitDetectionResult.noMatch();
        }

        ServiceProfitActionability actionability =
                resolveActionability(input);

        boolean suppressed =
                actionability == ServiceProfitActionability.SUPPRESSED;

        BigDecimal potentialAmount =
                input.recommendedAmount();

        return new ServiceProfitDetectionResult(
                true,
                suppressed,

                keyFactory.create(
                        input,
                        opportunityType
                ),

                opportunityType,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                resolvePriority(potentialAmount),
                actionability,

                titleFor(opportunityType),
                summaryFor(
                        opportunityType,
                        input
                ),

                potentialAmount,
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

    private ServiceProfitOpportunityType resolveOpportunityType(
            ServiceProfitDetectionInput input
    ) {
        if (input.disposition()
                == ServiceProfitDisposition.DECLINED) {
            return ServiceProfitOpportunityType.DECLINED_WORK;
        }

        if (input.disposition()
                == ServiceProfitDisposition.DEFERRED) {
            return ServiceProfitOpportunityType.DEFERRED_WORK;
        }

        if (input.recommendationStatus()
                == ServiceProfitRecommendationStatus.DECLINED) {
            return ServiceProfitOpportunityType.DECLINED_WORK;
        }

        if (input.recommendationStatus()
                == ServiceProfitRecommendationStatus.DEFERRED) {
            return ServiceProfitOpportunityType.DEFERRED_WORK;
        }

        return null;
    }

    private ServiceProfitActionability resolveActionability(
            ServiceProfitDetectionInput input
    ) {
        if (input.completedWorkEvidence()) {
            return ServiceProfitActionability.SUPPRESSED;
        }

        if (!input.customerContactable()) {
            return ServiceProfitActionability.CONTACT_DATA_MISSING;
        }

        return ServiceProfitActionability.READY;
    }

    private ServiceProfitPriority resolvePriority(
            BigDecimal potentialAmount
    ) {
        if (potentialAmount == null) {
            return ServiceProfitPriority.LOW;
        }

        if (potentialAmount.compareTo(HIGH_THRESHOLD) >= 0) {
            return ServiceProfitPriority.HIGH;
        }

        if (potentialAmount.compareTo(MEDIUM_THRESHOLD) >= 0) {
            return ServiceProfitPriority.MEDIUM;
        }

        return ServiceProfitPriority.LOW;
    }

    private String titleFor(
            ServiceProfitOpportunityType opportunityType
    ) {
        return switch (opportunityType) {
            case DECLINED_WORK ->
                    "Previously declined service opportunity";

            case DEFERRED_WORK ->
                    "Deferred service opportunity";

            default ->
                    throw new IllegalStateException(
                            "Unsupported explicit opportunity type: "
                                    + opportunityType
                    );
        };
    }

    private String summaryFor(
            ServiceProfitOpportunityType opportunityType,
            ServiceProfitDetectionInput input
    ) {
        String evidence =
                input.recommendationText() != null
                        ? input.recommendationText()
                        : input.advisorNote();

        if (evidence == null) {
            return switch (opportunityType) {
                case DECLINED_WORK ->
                        "Source data confirms previously declined service work.";

                case DEFERRED_WORK ->
                        "Source data confirms deferred service work.";

                default ->
                        null;
            };
        }

        return evidence;
    }
}
