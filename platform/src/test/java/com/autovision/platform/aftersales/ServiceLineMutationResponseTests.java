package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceLineMutationResponseTests {

    @Test
    void exposesCommercialSnapshotAndReadiness() {
        OffsetDateTime now = OffsetDateTime.now();
        ServiceLine line = newLine(now);
        line.applyCommercialSnapshot(
                new BigDecimal("10.00"),
                "USD",
                new BigDecimal("20.00"),
                new BigDecimal("4.00"),
                new BigDecimal("24.00"),
                UUID.randomUUID(),
                now.plusMinutes(1)
        );

        ServiceLineMutationResponse response =
                ServiceLineMutationResponse.from(line);

        assertEquals(new BigDecimal("10.00"), response.unitPrice());
        assertEquals("USD", response.currencyCode());
        assertEquals(new BigDecimal("20.00"), response.netAmount());
        assertEquals(new BigDecimal("4.00"), response.taxAmount());
        assertEquals(new BigDecimal("24.00"), response.grossAmount());
        assertTrue(response.hasCommercialSnapshot());
        assertNull(response.serviceJobId());
    }

    @Test
    void preservesNullCommercialValuesAndReportsIncompleteData() {
        ServiceLineMutationResponse response =
                ServiceLineMutationResponse.from(
                        newLine(OffsetDateTime.now())
                );

        assertNull(response.unitPrice());
        assertNull(response.currencyCode());
        assertNull(response.netAmount());
        assertNull(response.taxAmount());
        assertNull(response.grossAmount());
        assertFalse(response.hasCommercialSnapshot());
    }

    private ServiceLine newLine(OffsetDateTime now) {
        return ServiceLine.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                1,
                ServiceLineType.PART,
                "Brake pad",
                BigDecimal.ONE,
                "EA",
                UUID.randomUUID(),
                now
        );
    }
}