package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceDerivedServiceProfitDetectionPolicyTests {

    private final EvidenceDerivedServiceProfitDetectionPolicy policy =
            new EvidenceDerivedServiceProfitDetectionPolicy(
                    new ServiceProfitEvidencePhraseClassifier(),
                    new ServiceProfitOpportunityKeyFactory()
            );

    @Test
    void detectsStrongNoteDerivedDecline() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                "Front brake pads should be replaced.",
                                "Customer advised and will do next visit.",
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
                ServiceProfitEvidenceClass.EVIDENCE_DERIVED,
                result.evidenceClass()
        );

        assertEquals(
                ServiceProfitEvidenceStrength.STRONG,
                result.evidenceStrength()
        );

        assertEquals(
                ServiceProfitActionability.READY,
                result.actionability()
        );
    }

    @Test
    void ambiguousEvidenceRequiresReview() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                "Suspension attention recommended.",
                                "Recommended to customer.",
                                true,
                                false
                        )
                );

        assertTrue(result.detected());

        assertEquals(
                ServiceProfitEvidenceStrength.MODERATE,
                result.evidenceStrength()
        );

        assertEquals(
                ServiceProfitActionability.REVIEW_REQUIRED,
                result.actionability()
        );
    }

    @Test
    void ambiguousIdentityCreatesReviewRequiredCandidate() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                "Suspension attention recommended.",
                                "Customer details from previous DMS export could not be confidently matched.",
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
                ServiceProfitEvidenceClass.EVIDENCE_DERIVED,
                result.evidenceClass()
        );

        assertEquals(
                ServiceProfitEvidenceStrength.MODERATE,
                result.evidenceStrength()
        );

        assertEquals(
                ServiceProfitActionability.REVIEW_REQUIRED,
                result.actionability()
        );
    }

    @Test
    void completedEvidenceSuppressesOpportunity() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                "Battery replacement recommended.",
                                "Customer had deferred previously.",
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
    void missingContactDataChangesActionabilityOnly() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                "Brake pads should be replaced.",
                                "Customer declined work today.",
                                false,
                                false
                        )
                );

        assertTrue(result.detected());

        assertEquals(
                ServiceProfitEvidenceStrength.STRONG,
                result.evidenceStrength()
        );

        assertEquals(
                ServiceProfitActionability.CONTACT_DATA_MISSING,
                result.actionability()
        );
    }

    @Test
    void explicitDeclineIsLeftForExplicitPolicy() {

        ServiceProfitDetectionInput input =
                input(
                        "Brake pads should be replaced.",
                        "Customer declined work today.",
                        true,
                        false
                );

        input =
                new ServiceProfitDetectionInput(
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
                        ServiceProfitDisposition.DECLINED,
                        ServiceProfitRecommendationStatus.UNKNOWN,
                        input.recommendationText(),
                        input.advisorNote(),
                        input.recommendationDate(),
                        input.deferredUntilDate(),
                        input.nextServiceDueDate(),
                        input.lastServiceDate(),
                        input.currentMileage(),
                        input.nextServiceDueMileage(),
                        input.lastCustomerActivityAt(),
                        input.recommendedAmount(),
                        input.invoicedAmount(),
                        input.attributableCost(),
                        input.currencyCode(),
                        input.customerContactable(),
                        input.completedWorkEvidence(),
                        input.invoiceEvidence(),
                        input.evidence()
                );

        ServiceProfitDetectionResult result =
                policy.detect(input);

        assertFalse(result.detected());
    }

    @Test
    void recommendationWithoutSupportingNoteDoesNotMatch() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                "Brake pads should be replaced.",
                                null,
                                true,
                                false
                        )
                );

        assertFalse(result.detected());
    }

    private ServiceProfitDetectionInput input(
            String recommendationText,
            String note,
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
                "RECOMMENDATION",
                "REC-003",

                null,
                UUID.randomUUID(),
                null,
                null,

                ServiceProfitDisposition.UNKNOWN,
                ServiceProfitRecommendationStatus.UNKNOWN,

                recommendationText,
                note,

                null,
                null,
                null,
                null,

                null,
                null,

                null,

                new BigDecimal("9800.00"),
                null,
                null,
                "INR",

                contactable,
                completed,
                false,

                List.of(
                        new ServiceProfitEvidenceRef(
                                ServiceProfitEvidenceSourceType.RECOMMENDATION,
                                "REC-003",
                                null
                        ),
                        new ServiceProfitEvidenceRef(
                                ServiceProfitEvidenceSourceType.ADVISOR_NOTE,
                                "NOTE-003",
                                null
                        )
                )
        );
    }
}
