package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import com.autovision.platform.serviceprofit.ServiceProfitPriority;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplicitServiceProfitDetectionPolicyTests {

    private final ExplicitServiceProfitDetectionPolicy policy =
            new ExplicitServiceProfitDetectionPolicy(
                    new ServiceProfitOpportunityKeyFactory()
            );

    @Test
    void detectsExplicitDeclinedWork() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                ServiceProfitDisposition.DECLINED,
                                ServiceProfitRecommendationStatus.UNKNOWN,
                                new BigDecimal("12400.00"),
                                true,
                                false
                        )
                );

        assertTrue(result.detected());
        assertFalse(result.suppressed());

        assertEquals(
                ServiceProfitOpportunityType.DECLINED_WORK,
                result.opportunityType()
        );

        assertEquals(
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                result.evidenceClass()
        );

        assertEquals(
                ServiceProfitEvidenceStrength.STRONG,
                result.evidenceStrength()
        );

        assertEquals(
                ServiceProfitPriority.HIGH,
                result.priority()
        );

        assertEquals(
                ServiceProfitActionability.READY,
                result.actionability()
        );
    }

    @Test
    void detectsExplicitDeferredWork() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                ServiceProfitDisposition.DEFERRED,
                                ServiceProfitRecommendationStatus.UNKNOWN,
                                new BigDecimal("28000.00"),
                                true,
                                false
                        )
                );

        assertEquals(
                ServiceProfitOpportunityType.DEFERRED_WORK,
                result.opportunityType()
        );

        assertEquals(
                ServiceProfitActionability.READY,
                result.actionability()
        );
    }

    @Test
    void recommendationStatusCanProvideExplicitDeclineEvidence() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                ServiceProfitDisposition.UNKNOWN,
                                ServiceProfitRecommendationStatus.DECLINED,
                                new BigDecimal("6200.00"),
                                true,
                                false
                        )
                );

        assertTrue(result.detected());

        assertEquals(
                ServiceProfitOpportunityType.DECLINED_WORK,
                result.opportunityType()
        );

        assertEquals(
                ServiceProfitPriority.MEDIUM,
                result.priority()
        );
    }

    @Test
    void completedWorkIsSuppressed() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                ServiceProfitDisposition.DECLINED,
                                ServiceProfitRecommendationStatus.DECLINED,
                                new BigDecimal("8500.00"),
                                true,
                                true
                        )
                );

        assertTrue(result.detected());
        assertTrue(result.suppressed());

        assertEquals(
                ServiceProfitActionability.SUPPRESSED,
                result.actionability()
        );
    }

    @Test
    void missingContactDataDoesNotWeakenEvidence() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                ServiceProfitDisposition.DECLINED,
                                ServiceProfitRecommendationStatus.DECLINED,
                                new BigDecimal("3500.00"),
                                false,
                                false
                        )
                );

        assertEquals(
                ServiceProfitActionability.CONTACT_DATA_MISSING,
                result.actionability()
        );

        assertEquals(
                ServiceProfitEvidenceStrength.STRONG,
                result.evidenceStrength()
        );

        assertEquals(
                ServiceProfitPriority.LOW,
                result.priority()
        );
    }

    @Test
    void approvedOrUnknownInputDoesNotMatch() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                ServiceProfitDisposition.APPROVED,
                                ServiceProfitRecommendationStatus.APPROVED,
                                new BigDecimal("12000.00"),
                                true,
                                false
                        )
                );

        assertFalse(result.detected());
        assertFalse(result.suppressed());
    }

    @Test
    void opportunityKeyIsDeterministic() {

        ServiceProfitDetectionInput input =
                input(
                        ServiceProfitDisposition.DECLINED,
                        ServiceProfitRecommendationStatus.DECLINED,
                        new BigDecimal("12400.00"),
                        true,
                        false
                );

        ServiceProfitDetectionResult first =
                policy.detect(input);

        ServiceProfitDetectionResult second =
                policy.detect(input);

        assertEquals(
                first.opportunityKey(),
                second.opportunityKey()
        );
    }

    private ServiceProfitDetectionInput input(
            ServiceProfitDisposition disposition,
            ServiceProfitRecommendationStatus recommendationStatus,
            BigDecimal amount,
            boolean contactable,
            boolean completed
    ) {
        return new ServiceProfitDetectionInput(
                UUID.fromString(
                        "10000000-0000-4000-8000-000000000001"
                ),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),

                UUID.randomUUID(),
                UUID.randomUUID(),

                "DEMO-DMS",
                "SERVICE_JOB",
                "JOB-001",

                null,
                UUID.randomUUID(),
                null,
                null,

                disposition,
                recommendationStatus,

                "Brake or service work recommended.",
                "Customer advised.",

                null,
                null,
                null,
                null,

                null,
                null,

                null,

                amount,
                null,
                null,
                amount == null ? null : "INR",

                contactable,
                completed,
                false,

                List.of()
        );
    }
}
