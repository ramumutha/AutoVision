package com.autovision.platform.serviceprofit;

import java.math.BigDecimal;

public record ServiceProfitCurrencyPotential(
        String currencyCode,
        BigDecimal amount
) {
    public ServiceProfitCurrencyPotential {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency code is required"
            );
        }

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Potential amount is required"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Potential amount must not be negative"
            );
        }

        currencyCode = currencyCode.trim().toUpperCase();

        if (currencyCode.length() != 3) {
            throw new IllegalArgumentException(
                    "Currency code must contain exactly 3 characters"
            );
        }
    }
}