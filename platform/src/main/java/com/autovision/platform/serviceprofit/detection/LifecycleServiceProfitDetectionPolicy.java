package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import com.autovision.platform.serviceprofit.ServiceProfitPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public class LifecycleServiceProfitDetectionPolicy {

    private final ServiceProfitOpportunityKeyFactory keyFactory;
    private final ServiceProfitLifecyclePolicyConfig config;

    @Autowired
    public LifecycleServiceProfitDetectionPolicy(
            ServiceProfitOpportunityKeyFactory keyFactory
    ) {
        this(
                keyFactory,
                ServiceProfitLifecyclePolicyConfig.r1Default()
        );
    }

    LifecycleServiceProfitDetectionPolicy(
            ServiceProfitOpportunityKeyFactory keyFactory,
            ServiceProfitLifecyclePolicyConfig config
    ) {
        this.keyFactory = keyFactory;
        this.config = config;
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
                    "Lifecycle evaluation time is required"
            );
        }

        ServiceProfitOpportunityType type =
                resolveType(
                        input,
                        evaluatedAt
                );

        if (type == null) {
            return ServiceProfitDetectionResult.noMatch();
        }

        ServiceProfitActionability actionability =
                resolveActionability(input);

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
                ServiceProfitEvidenceClass.POLICY_DERIVED,
                resolveEvidenceStrength(
                        input,
                        type
                ),
                resolvePriority(type),
                actionability,

                titleFor(type),
                summaryFor(
                        input,
                        type,
                        evaluatedAt
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

                config.policyVersion()
        );
    }

    private ServiceProfitOpportunityType resolveType(
            ServiceProfitDetectionInput input,
            OffsetDateTime evaluatedAt
    ) {
        LocalDate evaluationDate =
                evaluatedAt.toLocalDate();

        if (isOverdue(
                input,
                evaluationDate
        )) {
            return ServiceProfitOpportunityType.OVERDUE_SERVICE;
        }

        if (isDue(
                input,
                evaluationDate
        )) {
            return ServiceProfitOpportunityType.DUE_SERVICE;
        }

        if (isInactive(
                input,
                evaluatedAt
        )) {
            return ServiceProfitOpportunityType.INACTIVE_CUSTOMER;
        }

        return null;
    }

    private boolean isOverdue(
            ServiceProfitDetectionInput input,
            LocalDate evaluationDate
    ) {
        boolean dateOverdue =
                input.nextServiceDueDate() != null
                        && input.nextServiceDueDate()
                        .isBefore(evaluationDate);

        boolean mileageOverdue =
                input.currentMileage() != null
                        && input.nextServiceDueMileage() != null
                        && input.currentMileage()
                        .compareTo(
                                input.nextServiceDueMileage()
                        ) > 0;

        return dateOverdue || mileageOverdue;
    }

    private boolean isDue(
            ServiceProfitDetectionInput input,
            LocalDate evaluationDate
    ) {
        boolean dateDue = false;

        if (input.nextServiceDueDate() != null) {
            LocalDate latestDueDate =
                    evaluationDate.plus(
                            config.dueDateWindow()
                    );

            dateDue =
                    !input.nextServiceDueDate()
                            .isBefore(evaluationDate)
                            && !input.nextServiceDueDate()
                            .isAfter(latestDueDate);
        }

        boolean mileageDue = false;

        if (input.currentMileage() != null
                && input.nextServiceDueMileage() != null) {

            BigDecimal lowerBound =
                    input.nextServiceDueMileage()
                            .subtract(
                                    config.dueMileageWindow()
                            );

            mileageDue =
                    input.currentMileage()
                            .compareTo(lowerBound) >= 0
                            && input.currentMileage()
                            .compareTo(
                                    input.nextServiceDueMileage()
                            ) <= 0;
        }

        return dateDue || mileageDue;
    }

    private boolean isInactive(
            ServiceProfitDetectionInput input,
            OffsetDateTime evaluatedAt
    ) {
        if (input.lastCustomerActivityAt() == null) {
            return false;
        }

        OffsetDateTime threshold =
                evaluatedAt.minus(
                        config.inactiveCustomerThreshold()
                );

        return input.lastCustomerActivityAt()
                .isBefore(threshold)
                || input.lastCustomerActivityAt()
                .isEqual(threshold);
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

    private ServiceProfitEvidenceStrength resolveEvidenceStrength(
            ServiceProfitDetectionInput input,
            ServiceProfitOpportunityType type
    ) {
        return switch (type) {

            case DUE_SERVICE,
                 OVERDUE_SERVICE -> {
                boolean hasDate =
                        input.nextServiceDueDate() != null;

                boolean hasMileage =
                        input.currentMileage() != null
                                && input.nextServiceDueMileage() != null;

                yield hasDate && hasMileage
                        ? ServiceProfitEvidenceStrength.STRONG
                        : ServiceProfitEvidenceStrength.MODERATE;
            }

            case INACTIVE_CUSTOMER ->
                    input.lastServiceDate() != null
                            ? ServiceProfitEvidenceStrength.STRONG
                            : ServiceProfitEvidenceStrength.MODERATE;

            default ->
                    ServiceProfitEvidenceStrength.MODERATE;
        };
    }

    private ServiceProfitPriority resolvePriority(
            ServiceProfitOpportunityType type
    ) {
        return switch (type) {
            case OVERDUE_SERVICE ->
                    ServiceProfitPriority.HIGH;

            case DUE_SERVICE ->
                    ServiceProfitPriority.MEDIUM;

            case INACTIVE_CUSTOMER ->
                    ServiceProfitPriority.MEDIUM;

            default ->
                    ServiceProfitPriority.LOW;
        };
    }

    private String titleFor(
            ServiceProfitOpportunityType type
    ) {
        return switch (type) {
            case DUE_SERVICE ->
                    "Upcoming service opportunity";

            case OVERDUE_SERVICE ->
                    "Overdue service opportunity";

            case INACTIVE_CUSTOMER ->
                    "Inactive customer recovery opportunity";

            default ->
                    throw new IllegalStateException(
                            "Unsupported lifecycle opportunity type: "
                                    + type
                    );
        };
    }

    private String summaryFor(
            ServiceProfitDetectionInput input,
            ServiceProfitOpportunityType type,
            OffsetDateTime evaluatedAt
    ) {
        return switch (type) {

            case DUE_SERVICE ->
                    "Service is approaching its configured due date or mileage threshold.";

            case OVERDUE_SERVICE ->
                    "Service has exceeded its configured due date or mileage threshold.";

            case INACTIVE_CUSTOMER ->
                    "Customer has had no recorded activity within the configured inactivity period.";

            default ->
                    null;
        };
    }
}




