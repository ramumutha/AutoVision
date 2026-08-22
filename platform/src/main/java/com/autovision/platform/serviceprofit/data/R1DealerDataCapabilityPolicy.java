package com.autovision.platform.serviceprofit.data;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class R1DealerDataCapabilityPolicy {

    public List<DealerDataCapabilityResult> evaluate(
            DealerDataFeatureProfile features
    ) {
        if (features == null) {
            throw new IllegalArgumentException(
                    "Dealer data feature profile is required"
            );
        }

        return List.of(
                evaluateExplicitDeclined(features),
                evaluateDeclinedReconstruction(features),
                evaluateDeferredWork(features),
                evaluateDueOverdue(features),
                evaluateInactiveCustomer(features),
                evaluateRevenueAttribution(features),
                evaluateGrossProfitAttribution(features)
        );
    }

    private DealerDataCapabilityResult evaluateExplicitDeclined(
            DealerDataFeatureProfile features
    ) {
        return switch (features.structuredDisposition()) {
            case AVAILABLE -> result(
                    DealerDataCapability.DECLINED_WORK_EXPLICIT,
                    DealerDataCapabilityStatus.AVAILABLE,
                    "Structured declined/rejected disposition data is consistently available."
            );
            case PARTIAL -> result(
                    DealerDataCapability.DECLINED_WORK_EXPLICIT,
                    DealerDataCapabilityStatus.PARTIAL,
                    "Structured declined/rejected disposition data is available for only part of the dealer dataset."
            );
            case UNAVAILABLE -> result(
                    DealerDataCapability.DECLINED_WORK_EXPLICIT,
                    DealerDataCapabilityStatus.UNAVAILABLE,
                    "No structured declined/rejected disposition data is available."
            );
        };
    }

    private DealerDataCapabilityResult evaluateDeclinedReconstruction(
            DealerDataFeatureProfile features
    ) {
        DealerDataFeatureAvailability notes =
                features.technicianAdvisorNotes();

        DealerDataFeatureAvailability recommendations =
                features.recommendationHistory();

        if (notes == DealerDataFeatureAvailability.AVAILABLE
                && recommendations == DealerDataFeatureAvailability.AVAILABLE) {
            return result(
                    DealerDataCapability.DECLINED_WORK_RECONSTRUCTION,
                    DealerDataCapabilityStatus.AVAILABLE,
                    "Advisor/technician notes and recommendation history provide strong reconstruction evidence."
            );
        }

        if (notes != DealerDataFeatureAvailability.UNAVAILABLE
                || recommendations != DealerDataFeatureAvailability.UNAVAILABLE) {
            return result(
                    DealerDataCapability.DECLINED_WORK_RECONSTRUCTION,
                    DealerDataCapabilityStatus.PARTIAL,
                    "Only part of the evidence required for declined-work reconstruction is consistently available."
            );
        }

        return result(
                DealerDataCapability.DECLINED_WORK_RECONSTRUCTION,
                DealerDataCapabilityStatus.UNAVAILABLE,
                "No usable notes or recommendation history are available for reconstruction."
        );
    }

    private DealerDataCapabilityResult evaluateDeferredWork(
            DealerDataFeatureProfile features
    ) {
        if (features.structuredDisposition()
                    == DealerDataFeatureAvailability.AVAILABLE
                || features.recommendationHistory()
                    == DealerDataFeatureAvailability.AVAILABLE) {
            return result(
                    DealerDataCapability.DEFERRED_WORK,
                    DealerDataCapabilityStatus.AVAILABLE,
                    "Disposition or recommendation history provides usable deferred-work evidence."
            );
        }

        if (features.structuredDisposition()
                    == DealerDataFeatureAvailability.PARTIAL
                || features.recommendationHistory()
                    == DealerDataFeatureAvailability.PARTIAL
                || features.technicianAdvisorNotes()
                    != DealerDataFeatureAvailability.UNAVAILABLE) {
            return result(
                    DealerDataCapability.DEFERRED_WORK,
                    DealerDataCapabilityStatus.PARTIAL,
                    "Deferred-work evidence exists but is incomplete or requires interpretation."
            );
        }

        return result(
                DealerDataCapability.DEFERRED_WORK,
                DealerDataCapabilityStatus.UNAVAILABLE,
                "No deferred-work evidence source is available."
        );
    }

    private DealerDataCapabilityResult evaluateDueOverdue(
            DealerDataFeatureProfile features
    ) {
        if (features.serviceHistory()
                    == DealerDataFeatureAvailability.AVAILABLE
                && features.mileageHistory()
                    == DealerDataFeatureAvailability.AVAILABLE) {
            return result(
                    DealerDataCapability.DUE_OVERDUE_SERVICE,
                    DealerDataCapabilityStatus.AVAILABLE,
                    "Service history and mileage history are consistently available."
            );
        }

        if (features.serviceHistory()
                    != DealerDataFeatureAvailability.UNAVAILABLE
                || features.mileageHistory()
                    != DealerDataFeatureAvailability.UNAVAILABLE) {
            return result(
                    DealerDataCapability.DUE_OVERDUE_SERVICE,
                    DealerDataCapabilityStatus.PARTIAL,
                    "Only part of the service timing evidence is consistently available."
            );
        }

        return result(
                DealerDataCapability.DUE_OVERDUE_SERVICE,
                DealerDataCapabilityStatus.UNAVAILABLE,
                "Service and mileage history are unavailable."
        );
    }

    private DealerDataCapabilityResult evaluateInactiveCustomer(
            DealerDataFeatureProfile features
    ) {
        if (features.customerActivityHistory()
                    == DealerDataFeatureAvailability.AVAILABLE
                && features.serviceHistory()
                    == DealerDataFeatureAvailability.AVAILABLE) {
            return result(
                    DealerDataCapability.INACTIVE_CUSTOMER,
                    DealerDataCapabilityStatus.AVAILABLE,
                    "Customer activity and service history are consistently available."
            );
        }

        if (features.customerActivityHistory()
                    != DealerDataFeatureAvailability.UNAVAILABLE
                || features.serviceHistory()
                    != DealerDataFeatureAvailability.UNAVAILABLE) {
            return result(
                    DealerDataCapability.INACTIVE_CUSTOMER,
                    DealerDataCapabilityStatus.PARTIAL,
                    "Customer inactivity can only be assessed with partial historical context."
            );
        }

        return result(
                DealerDataCapability.INACTIVE_CUSTOMER,
                DealerDataCapabilityStatus.UNAVAILABLE,
                "Customer activity and service history are unavailable."
        );
    }

    private DealerDataCapabilityResult evaluateRevenueAttribution(
            DealerDataFeatureProfile features
    ) {
        return switch (features.invoiceLinkage()) {
            case AVAILABLE -> result(
                    DealerDataCapability.REVENUE_ATTRIBUTION,
                    DealerDataCapabilityStatus.AVAILABLE,
                    "Invoice linkage is consistently available."
            );
            case PARTIAL -> result(
                    DealerDataCapability.REVENUE_ATTRIBUTION,
                    DealerDataCapabilityStatus.PARTIAL,
                    "Invoice linkage exists but is incomplete."
            );
            case UNAVAILABLE -> result(
                    DealerDataCapability.REVENUE_ATTRIBUTION,
                    DealerDataCapabilityStatus.UNAVAILABLE,
                    "Reliable invoice linkage is unavailable."
            );
        };
    }

    private DealerDataCapabilityResult evaluateGrossProfitAttribution(
            DealerDataFeatureProfile features
    ) {
        if (features.invoiceLinkage()
                    == DealerDataFeatureAvailability.AVAILABLE
                && features.costData()
                    == DealerDataFeatureAvailability.AVAILABLE) {
            return result(
                    DealerDataCapability.GROSS_PROFIT_ATTRIBUTION,
                    DealerDataCapabilityStatus.AVAILABLE,
                    "Invoice linkage and cost data are consistently available."
            );
        }

        if (features.invoiceLinkage()
                    != DealerDataFeatureAvailability.UNAVAILABLE
                && features.costData()
                    != DealerDataFeatureAvailability.UNAVAILABLE) {
            return result(
                    DealerDataCapability.GROSS_PROFIT_ATTRIBUTION,
                    DealerDataCapabilityStatus.PARTIAL,
                    "Gross-profit attribution is constrained by incomplete invoice or cost data."
            );
        }

        if (features.invoiceLinkage()
                    != DealerDataFeatureAvailability.UNAVAILABLE
                || features.costData()
                    != DealerDataFeatureAvailability.UNAVAILABLE) {
            return result(
                    DealerDataCapability.GROSS_PROFIT_ATTRIBUTION,
                    DealerDataCapabilityStatus.PARTIAL,
                    "Only one of invoice linkage or cost evidence is usable."
            );
        }

        return result(
                DealerDataCapability.GROSS_PROFIT_ATTRIBUTION,
                DealerDataCapabilityStatus.UNAVAILABLE,
                "Invoice and cost evidence required for gross-profit attribution are unavailable."
        );
    }

    private DealerDataCapabilityResult result(
            DealerDataCapability capability,
            DealerDataCapabilityStatus status,
            String reason
    ) {
        return new DealerDataCapabilityResult(
                capability,
                status,
                reason
        );
    }
}
