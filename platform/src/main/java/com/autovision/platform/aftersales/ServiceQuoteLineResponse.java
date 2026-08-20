package com.autovision.platform.aftersales;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceQuoteLineResponse(
        UUID id,
        UUID serviceQuoteId,
        UUID serviceLineId,
        UUID serviceJobId,
        String descriptionSnapshot,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String currencyCode,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal grossAmount,
        int sequence,
        UUID createdByPrincipalId,
        OffsetDateTime createdAt
) {
    public static ServiceQuoteLineResponse from(ServiceQuoteLine line) {
        return new ServiceQuoteLineResponse(
                line.getId(), line.getServiceQuoteId(), line.getServiceLineId(),
                line.getServiceJobId(), line.getDescriptionSnapshot(), line.getQuantity(),
                line.getUnitPrice(), line.getCurrencyCode(), line.getNetAmount(),
                line.getTaxAmount(), line.getGrossAmount(), line.getSequence(),
                line.getCreatedByPrincipalId(), line.getCreatedAt()
        );
    }
}