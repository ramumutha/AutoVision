package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceProfitOpportunitySuppressionTests {

    private static final UUID TENANT_ID =
            UUID.fromString(
                    "10000000-0000-4000-8000-000000000001"
            );

    private static final UUID PRINCIPAL_ID =
            UUID.fromString(
                    "20000000-0000-4000-8000-000000000001"
            );

    @Test
    void detectedOpportunityCanBeSuppressed() {

        ServiceProfitOpportunity opportunity =
                opportunity();

        OffsetDateTime suppressedAt =
                OffsetDateTime.parse(
                        "2026-08-22T10:00:00+05:30"
                );

        opportunity.suppress(
                ServiceProfitSuppressionReason.WORK_ALREADY_COMPLETED,
                PRINCIPAL_ID,
                suppressedAt
        );

        assertEquals(
                ServiceProfitOpportunityStatus.SUPPRESSED,
                opportunity.getStatus()
        );

        assertEquals(
                ServiceProfitActionability.SUPPRESSED,
                opportunity.getActionability()
        );

        assertEquals(
                ServiceProfitSuppressionReason.WORK_ALREADY_COMPLETED,
                opportunity.getSuppressionReason()
        );

        assertEquals(
                suppressedAt,
                opportunity.getSuppressedAt()
        );

        assertEquals(
                PRINCIPAL_ID,
                opportunity.getUpdatedByPrincipalId()
        );

        assertEquals(
                suppressedAt,
                opportunity.getUpdatedAt()
        );
    }

    @Test
    void suppressionRequiresReason() {

        ServiceProfitOpportunity opportunity =
                opportunity();

        assertThrows(
                IllegalArgumentException.class,
                () -> opportunity.suppress(
                        null,
                        PRINCIPAL_ID,
                        OffsetDateTime.parse(
                                "2026-08-22T10:00:00+05:30"
                        )
                )
        );
    }

    @Test
    void suppressionRequiresTimestamp() {

        ServiceProfitOpportunity opportunity =
                opportunity();

        assertThrows(
                IllegalArgumentException.class,
                () -> opportunity.suppress(
                        ServiceProfitSuppressionReason.WORK_ALREADY_COMPLETED,
                        PRINCIPAL_ID,
                        null
                )
        );
    }

    @Test
    void repeatedSuppressionIsIdempotent() {

        ServiceProfitOpportunity opportunity =
                opportunity();

        OffsetDateTime first =
                OffsetDateTime.parse(
                        "2026-08-22T10:00:00+05:30"
                );

        opportunity.suppress(
                ServiceProfitSuppressionReason.WORK_ALREADY_COMPLETED,
                PRINCIPAL_ID,
                first
        );

        opportunity.suppress(
                ServiceProfitSuppressionReason.ALREADY_INVOICED,
                UUID.fromString(
                        "30000000-0000-4000-8000-000000000001"
                ),
                first.plusHours(1)
        );

        assertEquals(
                ServiceProfitSuppressionReason.WORK_ALREADY_COMPLETED,
                opportunity.getSuppressionReason()
        );

        assertEquals(
                first,
                opportunity.getSuppressedAt()
        );

        assertEquals(
                PRINCIPAL_ID,
                opportunity.getUpdatedByPrincipalId()
        );
    }

    private ServiceProfitOpportunity opportunity() {

        OffsetDateTime detectedAt =
                OffsetDateTime.parse(
                        "2026-08-22T09:00:00+05:30"
                );

        return ServiceProfitOpportunity.detect(
                UUID.fromString(
                        "40000000-0000-4000-8000-000000000001"
                ),
                TENANT_ID,
                null,
                null,
                null,
                UUID.fromString(
                        "50000000-0000-4000-8000-000000000001"
                ),
                UUID.fromString(
                        "60000000-0000-4000-8000-000000000001"
                ),
                "SP:DECLINED_WORK:TEST:001",
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                "Declined brake work",
                "Customer declined recommended brake replacement.",
                null,
                null,
                "TEST-DMS",
                "RECOMMENDATION",
                "REC-001",
                null,
                null,
                null,
                null,
                "R1-TEST",
                PRINCIPAL_ID,
                detectedAt
        );
    }
}