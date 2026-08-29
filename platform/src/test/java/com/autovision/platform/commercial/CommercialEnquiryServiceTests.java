package com.autovision.platform.commercial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommercialEnquiryServiceTests {
    private final CommercialEnquiryRepository enquiries = mock(CommercialEnquiryRepository.class);
    private final CommercialVerificationRepository verifications = mock(CommercialVerificationRepository.class);
    private final CommercialVerificationDelivery delivery = mock(CommercialVerificationDelivery.class);
    private CommercialEnquiryService service;

    @BeforeEach
    void setUp() {
        service = new CommercialEnquiryService(enquiries, verifications, delivery,
                Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC));
        when(enquiries.existsByBusinessEmailAndPurposeAndProductAndCreatedAtAfter(any(), any(), any(), any())).thenReturn(false);
    }

    @Test
    void submitsDemoWithNormalizedFreeMailAndDigestOnly() {
        CommercialEnquiryResponse response = service.submit(new CommercialEnquiryRequest(
                CommercialEnquiryPurpose.PRODUCT_DEMO, " Dealer ", " Ada ", " Lovelace ", " ADA@GMAIL.COM ",
                "Owner", "IN", "See opportunities", CommercialProductFamily.AUTOVISION,
                CommercialProduct.SERVICE_PROFIT_AI, null), "127.0.0.1");

        assertEquals(CommercialEnquiryStatus.VERIFICATION_PENDING, response.status());
        ArgumentCaptor<CommercialEnquiry> enquiry = ArgumentCaptor.forClass(CommercialEnquiry.class);
        verify(enquiries).save(enquiry.capture());
        assertEquals("ada@gmail.com", enquiry.getValue().getBusinessEmail());
        assertEquals(CommercialEmailClassification.FREE_MAIL, enquiry.getValue().getEmailClassification());
        ArgumentCaptor<CommercialVerification> verification = ArgumentCaptor.forClass(CommercialVerification.class);
        verify(verifications).save(verification.capture());
        assertEquals(64, verification.getValue().getTokenDigest().length());
        verify(delivery).deliver(any(), org.mockito.ArgumentMatchers.eq("ada@gmail.com"), any());
    }

    @Test
    void rejectsUnsupportedPurposeSpecificValues() {
        CommercialEnquiryRequest request = new CommercialEnquiryRequest(
                CommercialEnquiryPurpose.PRODUCT_DEMO, "Dealer", "Ada", "Lovelace", "ada@dealer.example",
                "Owner", "IN", null, CommercialProductFamily.AUTOVISION, null, null);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.submit(request, "ip"));
        verify(enquiries, never()).save(any());
    }

    @Test
    void freeMailIsAllowedAndNonDemoProductIsRejected() {
        CommercialEnquiryRequest request = new CommercialEnquiryRequest(
                CommercialEnquiryPurpose.GENERAL_ENQUIRY, "Dealer", "Ada", "Lovelace", "ada@outlook.com",
                "Owner", "IN", "Question", CommercialProductFamily.AUTOVISION, CommercialProduct.SERVICE_PROFIT_AI, null);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.submit(request, "ip"));

        CommercialEnquiryRequest valid = new CommercialEnquiryRequest(
                CommercialEnquiryPurpose.GENERAL_ENQUIRY, "Dealer", "Ada", "Lovelace", "ada@outlook.com",
                "Owner", "IN", "Question", null, null, null);
        assertEquals(CommercialEnquiryStatus.VERIFICATION_PENDING, service.submit(valid, "other-ip").status());
    }

        @Test
        void duplicateIsReportedAsTooManyRequests() {
        when(enquiries.existsByBusinessEmailAndPurposeAndProductAndCreatedAtAfter(any(), any(), any(), any())).thenReturn(true);
        CommercialEnquiryRequest request = new CommercialEnquiryRequest(
            CommercialEnquiryPurpose.GENERAL_ENQUIRY, "Dealer", "Ada", "Lovelace", "ada@dealer.example",
            "Owner", "IN", "Question", null, null, null);

        org.springframework.web.server.ResponseStatusException exception = assertThrows(
            org.springframework.web.server.ResponseStatusException.class, () -> service.submit(request, "duplicate-ip"));

        assertEquals(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        verify(enquiries, never()).save(any());
        }

        @Test
        void eleventhSubmissionFromOneClientIsThrottled() {
        for (int index = 0; index < 10; index++) {
            CommercialEnquiryRequest request = new CommercialEnquiryRequest(
                CommercialEnquiryPurpose.GENERAL_ENQUIRY, "Dealer", "Ada", "Lovelace",
                "ada" + index + "@dealer.example", "Owner", "IN", "Question", null, null, null);
            service.submit(request, "rate-limit-ip");
        }
        CommercialEnquiryRequest eleventh = new CommercialEnquiryRequest(
            CommercialEnquiryPurpose.GENERAL_ENQUIRY, "Dealer", "Ada", "Lovelace", "ada10@dealer.example",
            "Owner", "IN", "Question", null, null, null);

        org.springframework.web.server.ResponseStatusException exception = assertThrows(
            org.springframework.web.server.ResponseStatusException.class, () -> service.submit(eleventh, "rate-limit-ip"));

        assertEquals(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        }

    @Test
    void verifiesOnlyOnceAndRejectsExpiredOrUnknownTokens() {
        String token = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        CommercialVerification verification = CommercialVerification.issue(UUID.randomUUID(), UUID.randomUUID(),
            CommercialEnquiryService.digest(token), java.time.OffsetDateTime.parse("2026-08-30T00:00:00Z"));
        when(verifications.findByTokenDigest(CommercialEnquiryService.digest(token))).thenReturn(Optional.of(verification));
        CommercialEnquiry enquiry = mock(CommercialEnquiry.class);
        when(enquiries.findById(verification.getEnquiryId())).thenReturn(Optional.of(enquiry));

        assertTrue(service.verify(token));
        assertFalse(service.verify(token));
        assertFalse(service.verify("unknown"));
    }
}
