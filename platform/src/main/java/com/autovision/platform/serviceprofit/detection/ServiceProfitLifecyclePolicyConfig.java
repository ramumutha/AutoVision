package com.autovision.platform.serviceprofit.detection;

import java.math.BigDecimal;
import java.time.Period;

public record ServiceProfitLifecyclePolicyConfig(
        Period dueDateWindow,
        BigDecimal dueMileageWindow,
        Period inactiveCustomerThreshold,
        String policyVersion
) {

    public ServiceProfitLifecyclePolicyConfig {

        if (dueDateWindow == null
                || dueDateWindow.isNegative()) {
            throw new IllegalArgumentException(
                    "Due date window is required and must not be negative"
            );
        }

        if (dueMileageWindow == null
                || dueMileageWindow.signum() < 0) {
            throw new IllegalArgumentException(
                    "Due mileage window is required and must not be negative"
            );
        }

        if (inactiveCustomerThreshold == null
                || inactiveCustomerThreshold.isNegative()
                || inactiveCustomerThreshold.isZero()) {
            throw new IllegalArgumentException(
                    "Inactive customer threshold must be positive"
            );
        }

        if (policyVersion == null
                || policyVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "Lifecycle policy version is required"
            );
        }
    }

    public static ServiceProfitLifecyclePolicyConfig r1Default() {
        return new ServiceProfitLifecyclePolicyConfig(
                Period.ofDays(30),
                new BigDecimal("1000"),
                Period.ofDays(365),
                "R1-LIFECYCLE-DETECTION-1"
        );
    }
}
