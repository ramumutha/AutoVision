package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceQuoteOperationPolicyTests {

    private final ServiceQuoteOperationPolicy policy =
            new DefaultServiceQuoteOperationPolicy();

    @Test
    void defaultPolicyAllowsEveryDefinedOperationForEveryStatus() {
        for (ServiceQuoteStatus status : ServiceQuoteStatus.values()) {
            for (ServiceQuoteOperation operation : ServiceQuoteOperation.values()) {
                assertTrue(policy.isAllowed(status, operation));
            }
        }
    }
}