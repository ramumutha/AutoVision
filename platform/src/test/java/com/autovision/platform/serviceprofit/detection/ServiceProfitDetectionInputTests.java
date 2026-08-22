package com.autovision.platform.serviceprofit.detection;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProfitDetectionInputTests {

    @Test
    void createsNormalizedDetectionInput() {

        UUID tenantId = UUID.randomUUID();

        ServiceProfitDetectionInput input =
                minimalInput(
                        tenantId,
                        new BigDecimal("12500.00"),
                        " inr "
                );

        assertEquals(tenantId, input.tenantId());
        assertEquals("DEMO-DMS", input.sourceSystem());
        assertEquals("RECOMMENDATION", input.sourceEntityType());
        assertEquals("REC-001", input.sourceEntityId());
        assertEquals("INR", input.currencyCode());

        assertEquals(
                new BigDecimal("12500.00"),
                input.recommendedAmount()
        );

        assertTrue(input.evidence().isEmpty());
    }

    @Test
    void rejectsMissingTenant() {

        assertThrows(
                IllegalArgumentException.class,
                () -> minimalInput(
                        null,
                        new BigDecimal("1000.00"),
                        "INR"
                )
        );
    }

    @Test
    void rejectsAmountWithoutCurrency() {

        assertThrows(
                IllegalArgumentException.class,
                () -> minimalInput(
                        UUID.randomUUID(),
                        new BigDecimal("1000.00"),
                        null
                )
        );
    }

    @Test
    void rejectsNegativeCommercialAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> minimalInput(
                        UUID.randomUUID(),
                        new BigDecimal("-1.00"),
                        "INR"
                )
        );
    }

    @Test
    void defensivelyCopiesEvidence() {

        java.util.ArrayList<ServiceProfitEvidenceRef> evidence =
                new java.util.ArrayList<>();

        evidence.add(
                new ServiceProfitEvidenceRef(
                        ServiceProfitEvidenceSourceType.RECOMMENDATION,
                        "REC-001",
                        null
                )
        );

        ServiceProfitDetectionInput input =
                create(
                        UUID.randomUUID(),
                        null,
                        "INR",
                        evidence
                );

        evidence.clear();

        assertEquals(1, input.evidence().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> input.evidence().add(
                        new ServiceProfitEvidenceRef(
                                ServiceProfitEvidenceSourceType.ADVISOR_NOTE,
                                "NOTE-001",
                                null
                        )
                )
        );
    }

    private ServiceProfitDetectionInput minimalInput(
            UUID tenantId,
            BigDecimal recommendedAmount,
            String currency
    ) {
        return create(
                tenantId,
                recommendedAmount,
                currency,
                List.of()
        );
    }

    private ServiceProfitDetectionInput create(
            UUID tenantId,
            BigDecimal recommendedAmount,
            String currency,
            List<ServiceProfitEvidenceRef> evidence
    ) {
        return new ServiceProfitDetectionInput(
                tenantId,
                null,
                null,
                null,

                UUID.randomUUID(),
                UUID.randomUUID(),

                " DEMO-DMS ",
                " RECOMMENDATION ",
                " REC-001 ",

                null,
                null,
                null,
                null,

                ServiceProfitDisposition.DECLINED,
                ServiceProfitRecommendationStatus.DECLINED,

                "Brake pads require replacement",
                null,

                null,
                null,
                null,
                null,

                null,
                null,

                null,

                recommendedAmount,
                null,
                null,
                currency,

                true,
                false,
                false,

                evidence
        );
    }
}
