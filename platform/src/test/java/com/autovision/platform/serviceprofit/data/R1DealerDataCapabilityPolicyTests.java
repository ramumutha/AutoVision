package com.autovision.platform.serviceprofit.data;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static com.autovision.platform.serviceprofit.data.DealerDataFeatureAvailability.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class R1DealerDataCapabilityPolicyTests {

    private final R1DealerDataCapabilityPolicy policy =
            new R1DealerDataCapabilityPolicy();

    @Test
    void supportsReconstructionWhenExplicitDispositionIsMissing() {

        DealerDataFeatureProfile features =
                new DealerDataFeatureProfile(
                        UNAVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        PARTIAL
                );

        Map<DealerDataCapability, DealerDataCapabilityStatus> statuses =
                statuses(features);

        assertEquals(
                DealerDataCapabilityStatus.UNAVAILABLE,
                statuses.get(
                        DealerDataCapability.DECLINED_WORK_EXPLICIT
                )
        );

        assertEquals(
                DealerDataCapabilityStatus.AVAILABLE,
                statuses.get(
                        DealerDataCapability.DECLINED_WORK_RECONSTRUCTION
                )
        );

        assertEquals(
                DealerDataCapabilityStatus.AVAILABLE,
                statuses.get(
                        DealerDataCapability.REVENUE_ATTRIBUTION
                )
        );

        assertEquals(
                DealerDataCapabilityStatus.PARTIAL,
                statuses.get(
                        DealerDataCapability.GROSS_PROFIT_ATTRIBUTION
                )
        );
    }

    @Test
    void partialStructuredDispositionProducesPartialExplicitCapability() {

        DealerDataFeatureProfile features =
                new DealerDataFeatureProfile(
                        PARTIAL,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        PARTIAL
                );

        Map<DealerDataCapability, DealerDataCapabilityStatus> statuses =
                statuses(features);

        assertEquals(
                DealerDataCapabilityStatus.PARTIAL,
                statuses.get(
                        DealerDataCapability.DECLINED_WORK_EXPLICIT
                )
        );

        assertEquals(
                DealerDataCapabilityStatus.AVAILABLE,
                statuses.get(
                        DealerDataCapability.DECLINED_WORK_RECONSTRUCTION
                )
        );

        assertEquals(
                DealerDataCapabilityStatus.PARTIAL,
                statuses.get(
                        DealerDataCapability.GROSS_PROFIT_ATTRIBUTION
                )
        );
    }

    @Test
    void fullFeatureProfileSupportsAllCapabilities() {

        DealerDataFeatureProfile features =
                new DealerDataFeatureProfile(
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE,
                        AVAILABLE
                );

        statuses(features).values().forEach(status ->
                assertEquals(
                        DealerDataCapabilityStatus.AVAILABLE,
                        status
                )
        );
    }

    private Map<DealerDataCapability, DealerDataCapabilityStatus> statuses(
            DealerDataFeatureProfile features
    ) {
        return policy.evaluate(features)
                .stream()
                .collect(
                        Collectors.toMap(
                                DealerDataCapabilityResult::capability,
                                DealerDataCapabilityResult::status
                        )
                );
    }
}
