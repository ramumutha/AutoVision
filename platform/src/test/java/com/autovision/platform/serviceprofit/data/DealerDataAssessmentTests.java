package com.autovision.platform.serviceprofit.data;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DealerDataAssessmentTests {

    @Test
    void createsExplainableDraftAssessment() {
        UUID principalId = UUID.randomUUID();

        OffsetDateTime now =
                OffsetDateTime.parse(
                        "2026-08-22T08:00:00Z"
                );

        DealerDataAssessment assessment =
                DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        "Dealer R1 extract",
                        "LEGACY_DMS",
                        null,
                        null,
                        coverage(),
                        new BigDecimal("78.25"),
                        "R1-DATA-READINESS-1",
                        principalId,
                        now
                );

        assertEquals(
                DealerDataAssessmentStatus.DRAFT,
                assessment.getStatus()
        );

        assertEquals(
                new BigDecimal("78.25"),
                assessment.getOverallScore()
        );

        assertEquals(
                principalId,
                assessment.getCreatedByPrincipalId()
        );

        assertNull(assessment.getAssessedAt());
        assertNull(assessment.getSourceDatasetId());
        assertNull(assessment.getSourceDatasetVersion());
    }

    @Test
    void createsAssessmentWithDatasetIdentity() {
        DealerDataAssessment assessment =
                DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        "AutoVision Service Profit R1 Demo",
                        "SYNTHETIC_DEMO",
                        "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                        "1.0.0",
                        coverage(),
                        new BigDecimal("78.25"),
                        "R1-DATA-READINESS-1",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                );

        assertEquals(
                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                assessment.getSourceDatasetId()
        );

        assertEquals(
                "1.0.0",
                assessment.getSourceDatasetVersion()
        );
    }

    @Test
    void rejectsIncompleteDatasetIdentityPair() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        "Dealer extract",
                        "LEGACY_DMS",
                        "DATASET-1",
                        null,
                        coverage(),
                        new BigDecimal("70.00"),
                        "R1-DATA-READINESS-1",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        "Dealer extract",
                        "LEGACY_DMS",
                        null,
                        "1.0.0",
                        coverage(),
                        new BigDecimal("70.00"),
                        "R1-DATA-READINESS-1",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void completesAssessmentWithAuditInformation() {
        UUID creator = UUID.randomUUID();
        UUID completer = UUID.randomUUID();

        DealerDataAssessment assessment =
                DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        "Dealer extract",
                        null,
                        null,
                        null,
                        coverage(),
                        new BigDecimal("70.00"),
                        "R1-DATA-READINESS-1",
                        creator,
                        OffsetDateTime.parse(
                                "2026-08-22T08:00:00Z"
                        )
                );

        OffsetDateTime completedAt =
                OffsetDateTime.parse(
                        "2026-08-22T09:00:00Z"
                );

        assessment.complete(
                completer,
                completedAt
        );

        assertEquals(
                DealerDataAssessmentStatus.COMPLETED,
                assessment.getStatus()
        );

        assertEquals(
                completedAt,
                assessment.getAssessedAt()
        );

        assertEquals(
                completer,
                assessment.getUpdatedByPrincipalId()
        );

        assertNotNull(assessment.getUpdatedAt());
    }

    @Test
    void rejectsBranchWithoutDealer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        UUID.randomUUID(),
                        "Dealer extract",
                        null,
                        null,
                        null,
                        coverage(),
                        new BigDecimal("70.00"),
                        "R1-DATA-READINESS-1",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void completedAssessmentCannotBeCompletedAgain() {
        DealerDataAssessment assessment =
                DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        "Dealer extract",
                        null,
                        null,
                        null,
                        coverage(),
                        new BigDecimal("70.00"),
                        "R1-DATA-READINESS-1",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                );

        assessment.complete(
                UUID.randomUUID(),
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> assessment.complete(
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    private DealerDataCoverage coverage() {
        return new DealerDataCoverage(
                new BigDecimal("95.00"),
                new BigDecimal("94.00"),
                new BigDecimal("98.00"),
                new BigDecimal("82.00"),
                BigDecimal.ZERO,
                new BigDecimal("71.00"),
                new BigDecimal("93.00"),
                new BigDecimal("35.00")
        );
    }
}
