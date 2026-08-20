package com.autovision.platform.aftersales;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceQuoteResponse(
        UUID id,
        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        UUID afterSalesCaseId,
        UUID serviceOrderId,
        String quoteNumber,
        ServiceQuoteStatus status,
        String currencyCode,
        OffsetDateTime validUntil,
        String termsSnapshot,
        String disclaimerSnapshot,
        OffsetDateTime issuedAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime declinedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime expiredAt,
        OffsetDateTime supersededAt,
        UUID createdByPrincipalId,
        UUID updatedByPrincipalId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ServiceQuoteLineResponse> lines
) {
    public static ServiceQuoteResponse from(
            ServiceQuote quote,
            List<ServiceQuoteLine> lines
    ) {
        return new ServiceQuoteResponse(
                quote.getId(), quote.getTenantId(), quote.getDealerId(), quote.getBranchId(),
                quote.getAfterSalesCaseId(), quote.getServiceOrderId(), quote.getQuoteNumber(),
                quote.getStatus(), quote.getCurrencyCode(), quote.getValidUntil(),
                quote.getTermsSnapshot(), quote.getDisclaimerSnapshot(), quote.getIssuedAt(),
                quote.getAcceptedAt(), quote.getDeclinedAt(), quote.getCancelledAt(),
                quote.getExpiredAt(), quote.getSupersededAt(), quote.getCreatedByPrincipalId(),
                quote.getUpdatedByPrincipalId(), quote.getCreatedAt(), quote.getUpdatedAt(),
                lines.stream().map(ServiceQuoteLineResponse::from).toList()
        );
    }
}