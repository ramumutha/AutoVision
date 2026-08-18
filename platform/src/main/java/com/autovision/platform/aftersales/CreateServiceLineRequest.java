package com.autovision.platform.aftersales;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateServiceLineRequest(
        UUID serviceJobId,
        int lineNumber,
        ServiceLineType lineType,
        String description,
        BigDecimal quantity,
        String unitOfMeasure
) {
}