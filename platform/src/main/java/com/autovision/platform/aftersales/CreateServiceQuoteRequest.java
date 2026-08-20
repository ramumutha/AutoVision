package com.autovision.platform.aftersales;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public record CreateServiceQuoteRequest(
        @NotBlank String quoteNumber,
        @NotBlank String currencyCode,
        OffsetDateTime validUntil,
        String termsSnapshot,
        String disclaimerSnapshot
) {
}