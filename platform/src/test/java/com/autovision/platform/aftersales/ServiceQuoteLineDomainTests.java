package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceQuoteLineDomainTests {

    @Test
    void createsValidLineAndPreservesSourceAndCommercialSnapshots() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceLineId = UUID.randomUUID();
        UUID serviceJobId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ServiceQuoteLine line = ServiceQuoteLine.create(
                UUID.randomUUID(), quoteId, serviceLineId, serviceJobId,
                "Replace brake pads", new BigDecimal("2.0000"),
                new BigDecimal("125.00"), "EUR", new BigDecimal("250.00"),
                new BigDecimal("50.00"), new BigDecimal("300.00"), 3,
                principalId, now
        );

        assertEquals(quoteId, line.getServiceQuoteId());
        assertEquals(serviceLineId, line.getServiceLineId());
        assertEquals(serviceJobId, line.getServiceJobId());
        assertEquals("Replace brake pads", line.getDescriptionSnapshot());
        assertEquals(0, new BigDecimal("2.0000").compareTo(line.getQuantity()));
        assertEquals(0, new BigDecimal("125.00").compareTo(line.getUnitPrice()));
        assertEquals("EUR", line.getCurrencyCode());
        assertEquals(0, new BigDecimal("250.00").compareTo(line.getNetAmount()));
        assertEquals(0, new BigDecimal("50.00").compareTo(line.getTaxAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(line.getGrossAmount()));
        assertEquals(3, line.getSequence());
        assertEquals(principalId, line.getCreatedByPrincipalId());
        assertEquals(now, line.getCreatedAt());
    }

    @Test
    void allowsMissingOptionalJobProvenance() {
        assertEquals(null, validLine(null).getServiceJobId());
    }

    @Test
    void rejectsMissingRequiredValuesAndNegativeSequence() {
        assertThrows(IllegalArgumentException.class, () -> create(null, UUID.randomUUID(), UUID.randomUUID(), null,
                "Description", BigDecimal.ONE, BigDecimal.ONE, "EUR", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), null, UUID.randomUUID(), null,
                "Description", BigDecimal.ONE, BigDecimal.ONE, "EUR", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), null, null,
                "Description", BigDecimal.ONE, BigDecimal.ONE, "EUR", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                " ", BigDecimal.ONE, BigDecimal.ONE, "EUR", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "Description", BigDecimal.ZERO, BigDecimal.ONE, "EUR", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "Description", BigDecimal.ONE, new BigDecimal("-1"), "EUR", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "Description", BigDecimal.ONE, BigDecimal.ONE, "EUR", new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "Description", BigDecimal.ONE, BigDecimal.ONE, "EUR", BigDecimal.ONE, new BigDecimal("-1"), BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "Description", BigDecimal.ONE, BigDecimal.ONE, "EUR", BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("-1"), 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "Description", BigDecimal.ONE, BigDecimal.ONE, " ", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "Description", BigDecimal.ONE, BigDecimal.ONE, "EUR", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, -1));
    }

    private ServiceQuoteLine validLine(UUID serviceJobId) {
        return create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), serviceJobId,
                "Description", BigDecimal.ONE, BigDecimal.ZERO, "USD",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }

    private ServiceQuoteLine create(
            UUID id, UUID quoteId, UUID lineId, UUID jobId, String description,
            BigDecimal quantity, BigDecimal unitPrice, String currency,
            BigDecimal net, BigDecimal tax, BigDecimal gross, int sequence
    ) {
        return ServiceQuoteLine.create(
                id, quoteId, lineId, jobId, description, quantity, unitPrice,
                currency, net, tax, gross, sequence, UUID.randomUUID(),
                OffsetDateTime.now()
        );
    }
}