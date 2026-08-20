package com.autovision.platform.aftersales;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceQuoteSummaryResponse(
        UUID id,
        UUID serviceOrderId,
        String quoteNumber,
        ServiceQuoteStatus status,
        String currencyCode,
        OffsetDateTime validUntil,
        OffsetDateTime issuedAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime declinedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime expiredAt,
        OffsetDateTime supersededAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ServiceQuoteSummaryResponse from(ServiceQuote quote) {
        return new ServiceQuoteSummaryResponse(
                quote.getId(), quote.getServiceOrderId(), quote.getQuoteNumber(),
                quote.getStatus(), quote.getCurrencyCode(), quote.getValidUntil(),
                quote.getIssuedAt(), quote.getAcceptedAt(), quote.getDeclinedAt(),
                quote.getCancelledAt(), quote.getExpiredAt(), quote.getSupersededAt(),
                quote.getCreatedAt(), quote.getUpdatedAt()
        );
    }
}