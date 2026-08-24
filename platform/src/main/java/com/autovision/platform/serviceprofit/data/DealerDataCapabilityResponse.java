package com.autovision.platform.serviceprofit.data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Tenant-safe Service Profit projection of the latest completed dealer
 * data-capability assessment.
 *
 * This contract intentionally avoids raw coverage percentages and overall
 * readiness scores because those values must not be interpreted as
 * opportunity-level confidence or dealer benchmarks.
 */
public record DealerDataCapabilityResponse(
        AssessmentState assessmentState,
        String sourceDatasetId,
        String sourceDatasetVersion,
        String assessmentPolicyVersion,
        OffsetDateTime assessedAt,
        List<DealerDataCapabilityItemResponse> capabilities
) {

    public enum AssessmentState {
        ASSESSED,
        NOT_ASSESSED
    }

    public DealerDataCapabilityResponse {
        capabilities = capabilities == null
                ? List.of()
                : List.copyOf(capabilities);
    }

    public static DealerDataCapabilityResponse notAssessed() {
        return new DealerDataCapabilityResponse(
                AssessmentState.NOT_ASSESSED,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    public static DealerDataCapabilityResponse assessed(
            DealerDataAssessment assessment,
            List<DealerDataCapabilityItemResponse> capabilities
    ) {
        if (assessment == null) {
            throw new IllegalArgumentException(
                    "Dealer data assessment is required"
            );
        }

        return new DealerDataCapabilityResponse(
                AssessmentState.ASSESSED,
                assessment.getSourceDatasetId(),
                assessment.getSourceDatasetVersion(),
                assessment.getAssessmentPolicyVersion(),
                assessment.getAssessedAt(),
                capabilities
        );
    }
}
