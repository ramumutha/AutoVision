package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceLineCommercialSnapshotDomainTests {

    @Test
    void existingServiceLineStartsWithoutCommercialSnapshot() {
        ServiceLine line = newLine();

        assertFalse(line.hasCommercialSnapshot());
        assertEquals(null, line.getUnitPrice());
        assertEquals(null, line.getCurrencyCode());
        assertEquals(null, line.getNetAmount());
        assertEquals(null, line.getTaxAmount());
        assertEquals(null, line.getGrossAmount());
    }

    @Test
    void appliesValidCommercialSnapshotAndPreservesAudit() {
        UUID createPrincipal = UUID.randomUUID();
        UUID updatePrincipal = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now().minusMinutes(5);
        OffsetDateTime updatedAt = OffsetDateTime.now();
        ServiceLine line = newLine(createPrincipal, createdAt);

        line.applyCommercialSnapshot(
                new BigDecimal("125.00"),
                "EUR",
                new BigDecimal("250.00"),
                new BigDecimal("50.00"),
                new BigDecimal("300.00"),
                updatePrincipal,
                updatedAt
        );

        assertEquals(0, new BigDecimal("125.00").compareTo(line.getUnitPrice()));
        assertEquals("EUR", line.getCurrencyCode());
        assertEquals(0, new BigDecimal("250.00").compareTo(line.getNetAmount()));
        assertEquals(0, new BigDecimal("50.00").compareTo(line.getTaxAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(line.getGrossAmount()));
        assertTrue(line.hasCommercialSnapshot());
        assertEquals(createPrincipal, line.getCreatedByPrincipalId());
        assertEquals(createdAt, line.getCreatedAt());
        assertEquals(updatePrincipal, line.getUpdatedByPrincipalId());
        assertEquals(updatedAt, line.getUpdatedAt());
        assertEquals(0, line.getVersion());
    }

    @Test
    void rejectsInvalidCommercialValuesAndAuditInputs() {
        assertThrows(IllegalArgumentException.class, () -> apply(
                new BigDecimal("-1"), "EUR", BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ONE, UUID.randomUUID(), OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> apply(
                BigDecimal.ONE, " ", BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ONE, UUID.randomUUID(), OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> apply(
                BigDecimal.ONE, "EUR", new BigDecimal("-1"),
                BigDecimal.ZERO, BigDecimal.ONE, UUID.randomUUID(), OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> apply(
                BigDecimal.ONE, "EUR", BigDecimal.ONE,
                new BigDecimal("-1"), BigDecimal.ONE, UUID.randomUUID(), OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> apply(
                BigDecimal.ONE, "EUR", BigDecimal.ONE,
                BigDecimal.ZERO, new BigDecimal("-1"), UUID.randomUUID(), OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> apply(
                BigDecimal.ONE, "EUR", BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ONE, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> apply(
                BigDecimal.ONE, "EUR", BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ONE, UUID.randomUUID(), null));
    }

    private void apply(
            BigDecimal unitPrice,
            String currencyCode,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal grossAmount,
            UUID principalId,
            OffsetDateTime now
    ) {
        newLine().applyCommercialSnapshot(
                unitPrice,
                currencyCode,
                netAmount,
                taxAmount,
                grossAmount,
                principalId,
                now
        );
    }

    private ServiceLine newLine() {
        return newLine(UUID.randomUUID(), OffsetDateTime.now());
    }

    private ServiceLine newLine(
            UUID principalId,
            OffsetDateTime now
    ) {
        return ServiceLine.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                1,
                ServiceLineType.LABOR,
                "Workshop labor",
                BigDecimal.ONE,
                "HOUR",
                principalId,
                now
        );
    }
}