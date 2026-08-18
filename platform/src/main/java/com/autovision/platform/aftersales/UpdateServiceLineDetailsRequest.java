package com.autovision.platform.aftersales;

import java.math.BigDecimal;

public record UpdateServiceLineDetailsRequest(
        ServiceLineType lineType,
        String description,
        BigDecimal quantity,
        String unitOfMeasure
) {
}