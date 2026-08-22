package com.autovision.platform.serviceprofit.demo;

import java.math.BigDecimal;

public record ServiceProfitDemoManifestCoverage(
        BigDecimal identity,
        BigDecimal vehicleLinkage,
        BigDecimal serviceTransaction,
        BigDecimal recommendationEvidence,
        BigDecimal disposition,
        BigDecimal mileage,
        BigDecimal invoiceLinkage,
        BigDecimal cost
) {
}
