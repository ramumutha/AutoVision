package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProfitOpportunityExplanationResolverTests {

    @Test
    void explainsSourceConfirmedDeclinedWork() {

        ServiceProfitOpportunity opportunity =
                opportunity(
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                        ServiceProfitEvidenceStrength.STRONG,
                        ServiceProfitActionability.READY
                );

        ServiceProfitOpportunityExplanation explanation =
                ServiceProfitOpportunityExplanationResolver.resolve(
                        opportunity
                );

        assertEquals(
                "Previously declined work may be recoverable",
                explanation.headline()
        );

        assertTrue(
                explanation.rationale()
                        .contains("declined")
        );

        assertTrue(
                explanation.evidenceBasis()
                        .contains("authoritative dealer source")
        );

        assertTrue(
                explanation.evidenceBasis()
                        .contains("strong")
        );

        assertTrue(
                explanation.recommendedAction()
                        .contains("customer follow-up")
        );
    }

    @Test
    void evidenceDerivedModerateOpportunityRequiresReview() {

        ServiceProfitOpportunity opportunity =
                opportunity(
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitEvidenceClass.EVIDENCE_DERIVED,
                        ServiceProfitEvidenceStrength.MODERATE,
                        ServiceProfitActionability.REVIEW_REQUIRED
                );

        ServiceProfitOpportunityExplanation explanation =
                ServiceProfitOpportunityExplanationResolver.resolve(
                        opportunity
                );

        assertTrue(
                explanation.evidenceBasis()
                        .contains("inferred")
        );

        assertTrue(
                explanation.evidenceBasis()
                        .contains("moderate")
        );

        assertEquals(
                "Review the supporting source information before contacting the customer.",
                explanation.recommendedAction()
        );
    }

    @Test
    void policyDerivedDueServiceExplainsLifecyclePolicy() {

        ServiceProfitOpportunity opportunity =
                opportunity(
                        ServiceProfitOpportunityType.DUE_SERVICE,
                        ServiceProfitEvidenceClass.POLICY_DERIVED,
                        ServiceProfitEvidenceStrength.STRONG,
                        ServiceProfitActionability.READY
                );

        ServiceProfitOpportunityExplanation explanation =
                ServiceProfitOpportunityExplanationResolver.resolve(
                        opportunity
                );

        assertEquals(
                "Scheduled service is due",
                explanation.headline()
        );

        assertTrue(
                explanation.evidenceBasis()
                        .contains("lifecycle policy")
        );
    }

    @Test
    void suppressionReasonProducesSafeRecommendedAction() {

        ServiceProfitOpportunity opportunity =
                opportunity(
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                        ServiceProfitEvidenceStrength.STRONG,
                        ServiceProfitActionability.READY
                );

        opportunity.suppress(
                ServiceProfitSuppressionReason.WORK_ALREADY_COMPLETED,
                UUID.randomUUID(),
                OffsetDateTime.parse(
                        "2026-08-22T10:00:00Z"
                )
        );

        ServiceProfitOpportunityExplanation explanation =
                ServiceProfitOpportunityExplanationResolver.resolve(
                        opportunity
                );

        assertTrue(
                explanation.rationale()
                        .contains("suppressed")
        );

        assertEquals(
                "No follow-up is required because the work is already recorded as completed.",
                explanation.recommendedAction()
        );
    }

    @Test
    void rejectsNullOpportunity() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ServiceProfitOpportunityExplanationResolver
                                .resolve(null)
        );
    }

    private ServiceProfitOpportunity opportunity(
            ServiceProfitOpportunityType opportunityType,
            ServiceProfitEvidenceClass evidenceClass,
            ServiceProfitEvidenceStrength evidenceStrength,
            ServiceProfitActionability actionability
    ) {
        return ServiceProfitOpportunity.detect(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "R1.5.2:" + UUID.randomUUID(),
                opportunityType,
                evidenceClass,
                evidenceStrength,
                ServiceProfitPriority.HIGH,
                actionability,
                "Service Profit opportunity",
                "Deterministic test opportunity.",
                new BigDecimal("250.0000"),
                "USD",
                "DEALER_IMPORT",
                "RO_LINE",
                UUID.randomUUID().toString(),
                null,
                null,
                null,
                null,
                "R1-POLICY-1",
                UUID.randomUUID(),
                OffsetDateTime.parse(
                        "2026-08-22T09:00:00Z"
                )
        );
    }
}