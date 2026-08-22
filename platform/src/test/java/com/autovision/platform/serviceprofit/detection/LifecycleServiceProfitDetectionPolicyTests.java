package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import com.autovision.platform.serviceprofit.ServiceProfitPriority;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleServiceProfitDetectionPolicyTests {

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse(
                    "2026-08-22T08:00:00Z"
            );

    private final LifecycleServiceProfitDetectionPolicy policy =
            new LifecycleServiceProfitDetectionPolicy(
                    new ServiceProfitOpportunityKeyFactory(),
                    ServiceProfitLifecyclePolicyConfig.r1Default()
            );

    @Test
    void detectsDueServiceByDate() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                LocalDate.of(
                                        2026,
                                        8,
                                        25
                                ),
                                new BigDecimal("30000"),
                                new BigDecimal("28000"),
                                NOW.minusDays(100),
                                true,
                                false
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

        assertEquals(
                ServiceProfitPriority.MEDIUM,
                result.priority()
        );
    }

    @Test
    void detectsDueServiceByMileageWindow() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                LocalDate.of(
                                        2027,
                                        1,
                                        1
                                ),
                                new BigDecimal("30000"),
                                new BigDecimal("29200"),
                                NOW.minusDays(100),
                                true,
                                false
                        ),
                        NOW
                );

        assertEquals(
                ServiceProfitOpportunityType.DUE_SERVICE,
                result.opportunityType()
        );
    }

    @Test
    void detectsOverdueServiceByDate() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                LocalDate.of(
                                        2026,
                                        4,
                                        10
                                ),
                                new BigDecimal("88000"),
                                new BigDecimal("85000"),
                                NOW.minusDays(100),
                                true,
                                false
                        ),
                        NOW
                );

        assertEquals(
                ServiceProfitOpportunityType.OVERDUE_SERVICE,
                result.opportunityType()
        );

        assertEquals(
                ServiceProfitPriority.HIGH,
                result.priority()
        );

        assertEquals(
                ServiceProfitEvidenceStrength.STRONG,
                result.evidenceStrength()
        );
    }

    @Test
    void mileageOverdueTakesPrecedenceOverFutureDate() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                LocalDate.of(
                                        2026,
                                        12,
                                        1
                                ),
                                new BigDecimal("50000"),
                                new BigDecimal("51000"),
                                NOW.minusDays(50),
                                true,
                                false
                        ),
                        NOW
                );

        assertEquals(
                ServiceProfitOpportunityType.OVERDUE_SERVICE,
                result.opportunityType()
        );
    }

    @Test
    void detectsInactiveCustomer() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                null,
                                null,
                                null,
                                NOW.minusDays(500),
                                true,
                                false
                        ),
                        NOW
                );

        assertEquals(
                ServiceProfitOpportunityType.INACTIVE_CUSTOMER,
                result.opportunityType()
        );

        assertEquals(
                ServiceProfitActionability.READY,
                result.actionability()
        );
    }

    @Test
    void missingContactDataChangesActionability() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                null,
                                null,
                                null,
                                NOW.minusDays(500),
                                false,
                                false
                        ),
                        NOW
                );

        assertEquals(
                ServiceProfitActionability.CONTACT_DATA_MISSING,
                result.actionability()
        );
    }

    @Test
    void completedEvidenceSuppressesLifecycleOpportunity() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                LocalDate.of(
                                        2026,
                                        8,
                                        25
                                ),
                                new BigDecimal("30000"),
                                new BigDecimal("29500"),
                                NOW.minusDays(100),
                                true,
                                true
                        ),
                        NOW
                );

        assertTrue(result.detected());
        assertTrue(result.suppressed());

        assertEquals(
                ServiceProfitActionability.SUPPRESSED,
                result.actionability()
        );
    }

    @Test
    void customerInsideInactivityThresholdDoesNotMatch() {

        ServiceProfitDetectionResult result =
                policy.detect(
                        input(
                                null,
                                null,
                                null,
                                NOW.minusDays(200),
                                true,
                                false
                        ),
                        NOW
                );

        assertFalse(result.detected());
    }

    private ServiceProfitDetectionInput input(
            LocalDate dueDate,
            BigDecimal dueMileage,
            BigDecimal currentMileage,
            OffsetDateTime lastActivity,
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
                "SERVICE_HISTORY",
                "HISTORY-001",

                null,
                null,
                null,
                null,

                ServiceProfitDisposition.UNKNOWN,
                ServiceProfitRecommendationStatus.UNKNOWN,

                null,
                null,

                null,
                null,
                dueDate,
                LocalDate.of(
                        2025,
                        10,
                        1
                ),

                currentMileage,
                dueMileage,

                lastActivity,

                null,
                null,
                null,
                null,

                contactable,
                completed,
                false,

                List.of(
                        new ServiceProfitEvidenceRef(
                                ServiceProfitEvidenceSourceType.SERVICE_HISTORY,
                                "HISTORY-001",
                                null
                        )
                )
        );
    }
}
