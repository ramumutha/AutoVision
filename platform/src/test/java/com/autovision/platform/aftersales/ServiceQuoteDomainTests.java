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
        void supportsConfigurableStatusChangesWithoutHardCodedGraph() {
                ServiceQuote quote = newQuote();
                UUID principalId = UUID.randomUUID();
                OffsetDateTime now = OffsetDateTime.now();

                quote.changeStatus(ServiceQuoteStatus.ISSUED, principalId, now);
                quote.changeStatus(ServiceQuoteStatus.ACCEPTED, principalId, now.plusMinutes(1));
                quote.changeStatus(ServiceQuoteStatus.CANCELLED, principalId, now.plusMinutes(2));
                quote.changeStatus(ServiceQuoteStatus.SUPERSEDED, principalId, now.plusMinutes(3));

                assertEquals(ServiceQuoteStatus.SUPERSEDED, quote.getStatus());
                assertEquals(now, quote.getIssuedAt());
                assertEquals(now.plusMinutes(1), quote.getAcceptedAt());
                assertEquals(now.plusMinutes(2), quote.getCancelledAt());
                assertEquals(now.plusMinutes(3), quote.getSupersededAt());
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
    void rejectsNoOpStatusChanges() {
        ServiceQuote quote = newQuote();
        OffsetDateTime now = OffsetDateTime.now();

        assertThrows(IllegalStateException.class,
                () -> quote.changeStatus(
                        ServiceQuoteStatus.DRAFT,
                        UUID.randomUUID(),
                        now
                ));
    }

    @Test
    void supportsReopenToDraftAtDomainLevelAndPreservesHistory() {
        ServiceQuote quote = newQuote();
        OffsetDateTime issuedAt = OffsetDateTime.now();
        OffsetDateTime acceptedAt = issuedAt.plusMinutes(1);

        quote.changeStatus(ServiceQuoteStatus.ISSUED, UUID.randomUUID(), issuedAt);
        quote.changeStatus(ServiceQuoteStatus.ACCEPTED, UUID.randomUUID(), acceptedAt);
        quote.changeStatus(ServiceQuoteStatus.DRAFT, UUID.randomUUID(), acceptedAt.plusMinutes(1));

        assertEquals(ServiceQuoteStatus.DRAFT, quote.getStatus());
        assertEquals(issuedAt, quote.getIssuedAt());
        assertEquals(acceptedAt, quote.getAcceptedAt());
    }

    @Test
    void rejectsInvalidStatusMutationArguments() {
        ServiceQuote quote = newQuote();
        OffsetDateTime now = OffsetDateTime.now();

        assertThrows(IllegalArgumentException.class,
                () -> quote.changeStatus(null, UUID.randomUUID(), now));
        assertThrows(IllegalArgumentException.class,
                () -> quote.changeStatus(ServiceQuoteStatus.ISSUED, null, now));
        assertThrows(IllegalArgumentException.class,
                () -> quote.changeStatus(ServiceQuoteStatus.ISSUED, UUID.randomUUID(), null));
    }

    @Test
    void convenienceMethodsDelegateToGenericMutation() {
        ServiceQuote quote = newQuote();
        quote.issue(UUID.randomUUID(), OffsetDateTime.now());
        quote.accept(UUID.randomUUID(), OffsetDateTime.now());
        quote.cancel(UUID.randomUUID(), OffsetDateTime.now());
        quote.supersede(UUID.randomUUID(), OffsetDateTime.now());

        assertEquals(ServiceQuoteStatus.SUPERSEDED, quote.getStatus());
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

    private ServiceQuote newQuote() {
        return ServiceQuote.create(
                UUID.randomUUID(), UUID.randomUUID(), null, null,
                UUID.randomUUID(), UUID.randomUUID(), "Q-1", "USD",
                null, null, null, UUID.randomUUID(), OffsetDateTime.now()
        );
    }
}