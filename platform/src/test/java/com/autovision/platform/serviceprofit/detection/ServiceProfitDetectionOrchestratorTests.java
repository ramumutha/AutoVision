package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProfitDetectionOrchestratorTests {

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse(
                    "2026-08-22T10:00:00+05:30"
            );

    private final ServiceProfitOpportunityKeyFactory keyFactory =
            new ServiceProfitOpportunityKeyFactory();

    private final ServiceProfitDetectionOrchestrator orchestrator =
            new ServiceProfitDetectionOrchestrator(
                    new ExplicitServiceProfitDetectionPolicy(
                            keyFactory
                    ),
                    new EvidenceDerivedServiceProfitDetectionPolicy(
                            new ServiceProfitEvidencePhraseClassifier(),
                            keyFactory
                    ),
                    new LifecycleServiceProfitDetectionPolicy(
                            keyFactory,
                            ServiceProfitLifecyclePolicyConfig.r1Default()
                    )
            );

    @Test
    void explicitDetectionTakesPrecedence() {

        ServiceProfitDetectionResult result =
                orchestrator.detect(
                        input(
                                ServiceProfitDisposition.DECLINED,
                                "Brake replacement recommended.",
                                "Customer advised and will do next visit.",
                                LocalDate.of(2026, 8, 25),
                                NOW.minusDays(500)
                        ),
                        NOW
                );

        assertTrue(result.detected());

        assertEquals(
                ServiceProfitOpportunityType.DECLINED_WORK,
                result.opportunityType()
        );

        assertEquals(
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                result.evidenceClass()
        );
    }

    @Test
    void evidenceDerivedDetectionPrecedesLifecycle() {

        ServiceProfitDetectionResult result =
                orchestrator.detect(
                        input(
                                null,
                                "Brake replacement recommended.",
                                "Customer advised and will do next visit.",
                                LocalDate.of(2026, 8, 25),
                                NOW.minusDays(500)
                        ),
                        NOW
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
    }

    @Test
    void lifecycleRunsWhenNoSourceEvidenceMatches() {

        ServiceProfitDetectionResult result =
                orchestrator.detect(
                        input(
                                null,
                                null,
                                null,
                                LocalDate.of(2026, 8, 25),
                                NOW.minusDays(100)
                        ),
                        NOW
                );

        assertTrue(result.detected());

        assertEquals(
                ServiceProfitOpportunityType.DUE_SERVICE,
                result.opportunityType()
        );

        assertEquals(
                ServiceProfitEvidenceClass.POLICY_DERIVED,
                result.evidenceClass()
        );
    }

    @Test
    void returnsNoMatchWhenNoPolicyMatches() {

        ServiceProfitDetectionResult result =
                orchestrator.detect(
                        input(
                                null,
                                null,
                                null,
                                null,
                                NOW.minusDays(50)
                        ),
                        NOW
                );

        assertFalse(result.detected());
    }

    private ServiceProfitDetectionInput input(
            ServiceProfitDisposition disposition,
            String recommendationText,
            String advisorNote,
            LocalDate dueDate,
            OffsetDateTime lastActivity
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

                "TEST-DMS",
                "SERVICE_JOB",
                "JOB-001",

                null,
                UUID.randomUUID(),
                null,
                null,

                disposition,
                null,

                recommendationText,
                advisorNote,

                null,
                null,
                dueDate,
                LocalDate.of(
                        2026,
                        1,
                        1
                ),

                dueDate == null
                        ? null
                        : new BigDecimal("29500"),

                dueDate == null
                        ? null
                        : new BigDecimal("30000"),

                lastActivity,

                recommendationText == null
                        ? null
                        : new BigDecimal("12000.00"),

                null,
                null,

                recommendationText == null
                        ? null
                        : "INR",

                true,
                false,
                false,

                List.of()
        );
    }
}