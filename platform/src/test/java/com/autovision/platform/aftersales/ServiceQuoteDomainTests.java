package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceQuoteDomainTests {

    @Test
    void createsValidDraftQuoteAndPreservesSnapshotsAndContainment() {
        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ServiceQuote quote = ServiceQuote.create(
                UUID.randomUUID(), tenantId, dealerId, branchId, caseId,
                orderId, "Q-100", "EUR", now.plusDays(30),
                "Payment due on completion", "Estimate subject to inspection",
                principalId, now
        );

        assertEquals(tenantId, quote.getTenantId());
        assertEquals(dealerId, quote.getDealerId());
        assertEquals(branchId, quote.getBranchId());
        assertEquals(caseId, quote.getAfterSalesCaseId());
        assertEquals(orderId, quote.getServiceOrderId());
        assertEquals("Q-100", quote.getQuoteNumber());
        assertEquals("EUR", quote.getCurrencyCode());
        assertEquals("Payment due on completion", quote.getTermsSnapshot());
        assertEquals("Estimate subject to inspection", quote.getDisclaimerSnapshot());
        assertEquals(ServiceQuoteStatus.DRAFT, quote.getStatus());
        assertEquals(principalId, quote.getCreatedByPrincipalId());
        assertEquals(principalId, quote.getUpdatedByPrincipalId());
        assertEquals(now, quote.getCreatedAt());
        assertEquals(now, quote.getUpdatedAt());
    }

    @Test
    void supportsEachIssuedTerminalDecision() {
        assertTerminalTransition(ServiceQuoteStatus.ACCEPTED, "accept");
        assertTerminalTransition(ServiceQuoteStatus.DECLINED, "decline");
        assertTerminalTransition(ServiceQuoteStatus.CANCELLED, "cancel");
        assertTerminalTransition(ServiceQuoteStatus.EXPIRED, "expire");
        assertTerminalTransition(ServiceQuoteStatus.SUPERSEDED, "supersede");
    }

    @Test
    void issueRecordsStatusTimestampAndActingPrincipal() {
        ServiceQuote quote = newQuote();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.now();

        quote.issue(principalId, issuedAt);

        assertEquals(ServiceQuoteStatus.ISSUED, quote.getStatus());
        assertEquals(issuedAt, quote.getIssuedAt());
        assertEquals(principalId, quote.getUpdatedByPrincipalId());
        assertEquals(issuedAt, quote.getUpdatedAt());
    }

    @Test
    void rejectsRepeatedAndTerminalTransitions() {
        ServiceQuote quote = newQuote();
        quote.issue(UUID.randomUUID(), OffsetDateTime.now());
        quote.accept(UUID.randomUUID(), OffsetDateTime.now());

        assertThrows(IllegalStateException.class,
                () -> quote.accept(UUID.randomUUID(), OffsetDateTime.now()));
        assertThrows(IllegalStateException.class,
                () -> quote.decline(UUID.randomUUID(), OffsetDateTime.now()));
        assertThrows(IllegalStateException.class,
                () -> quote.issue(UUID.randomUUID(), OffsetDateTime.now()));
    }

    @Test
    void rejectsMissingRequiredValuesAndInvalidContainment() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        assertThrows(IllegalArgumentException.class, () -> ServiceQuote.create(
                null, tenantId, null, null, caseId, orderId,
                "Q-1", "EUR", null, null, null, principalId, now));
        assertThrows(IllegalArgumentException.class, () -> ServiceQuote.create(
                id, null, null, null, caseId, orderId,
                "Q-1", "EUR", null, null, null, principalId, now));
        assertThrows(IllegalArgumentException.class, () -> ServiceQuote.create(
                id, tenantId, null, null, null, orderId,
                "Q-1", "EUR", null, null, null, principalId, now));
        assertThrows(IllegalArgumentException.class, () -> ServiceQuote.create(
                id, tenantId, null, null, caseId, null,
                "Q-1", "EUR", null, null, null, principalId, now));
        assertThrows(IllegalArgumentException.class, () -> ServiceQuote.create(
                id, tenantId, null, null, caseId, orderId,
                " ", "EUR", null, null, null, principalId, now));
        assertThrows(IllegalArgumentException.class, () -> ServiceQuote.create(
                id, tenantId, null, null, caseId, orderId,
                "Q-1", " ", null, null, null, principalId, now));
        assertThrows(IllegalArgumentException.class, () -> ServiceQuote.create(
                id, tenantId, null, UUID.randomUUID(), caseId, orderId,
                "Q-1", "EUR", null, null, null, principalId, now));
    }

    private void assertTerminalTransition(
            ServiceQuoteStatus expectedStatus,
            String operation
    ) {
        ServiceQuote quote = newQuote();
        OffsetDateTime now = OffsetDateTime.now();
        UUID principalId = UUID.randomUUID();
        quote.issue(UUID.randomUUID(), now.minusMinutes(1));

        switch (operation) {
            case "accept" -> quote.accept(principalId, now);
            case "decline" -> quote.decline(principalId, now);
            case "cancel" -> quote.cancel(principalId, now);
            case "expire" -> quote.expire(principalId, now);
            case "supersede" -> quote.supersede(principalId, now);
            default -> throw new AssertionError("Unknown operation");
        }

        assertEquals(expectedStatus, quote.getStatus());
        assertEquals(principalId, quote.getUpdatedByPrincipalId());
        assertEquals(now, quote.getUpdatedAt());
        assertTrue(expectedStatus != ServiceQuoteStatus.DRAFT);
    }

    private ServiceQuote newQuote() {
        return ServiceQuote.create(
                UUID.randomUUID(), UUID.randomUUID(), null, null,
                UUID.randomUUID(), UUID.randomUUID(), "Q-1", "USD",
                null, null, null, UUID.randomUUID(), OffsetDateTime.now()
        );
    }
}