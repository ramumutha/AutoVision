package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceQuoteLineEligibilityPolicyTests {

    private final ServiceQuoteLineEligibilityPolicy policy =
            new DefaultServiceQuoteLineEligibilityPolicy();

    @Test
    void blocksDefaultBlockingStatuses() {
        assertTrue(policy.blocksRequotation(ServiceQuoteStatus.DRAFT));
        assertTrue(policy.blocksRequotation(ServiceQuoteStatus.ISSUED));
        assertTrue(policy.blocksRequotation(ServiceQuoteStatus.ACCEPTED));
    }

    @Test
    void releasesDefaultReleasingStatuses() {
        assertFalse(policy.blocksRequotation(ServiceQuoteStatus.DECLINED));
        assertFalse(policy.blocksRequotation(ServiceQuoteStatus.CANCELLED));
        assertFalse(policy.blocksRequotation(ServiceQuoteStatus.EXPIRED));
        assertFalse(policy.blocksRequotation(ServiceQuoteStatus.SUPERSEDED));
    }

    @Test
    void handlesNullDefensively() {
        assertFalse(policy.blocksRequotation(null));
    }
}