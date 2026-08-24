package com.autovision.platform.serviceprofit.data;

/**
 * Dealer-facing projection of one assessed data capability.
 *
 * The response deliberately exposes only the capability result required
 * for Service Profit decision support. Raw coverage metrics remain an
 * internal assessment concern.
 */
public record DealerDataCapabilityItemResponse(
        DealerDataCapability capability,
        DealerDataCapabilityStatus status,
        String reason
) {

    public static DealerDataCapabilityItemResponse from(
            DealerDataCapabilityAssessment assessment
    ) {
        if (assessment == null) {
            throw new IllegalArgumentException(
                    "Dealer data capability assessment is required"
            );
        }

        return new DealerDataCapabilityItemResponse(
                assessment.getCapability(),
                assessment.getStatus(),
                assessment.getReason()
        );
    }
}
