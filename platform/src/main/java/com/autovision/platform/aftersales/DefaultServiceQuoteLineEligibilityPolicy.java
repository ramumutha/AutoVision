package com.autovision.platform.aftersales;

import org.springframework.stereotype.Component;

@Component
public class DefaultServiceQuoteLineEligibilityPolicy
        implements ServiceQuoteLineEligibilityPolicy {

    @Override
    public boolean blocksRequotation(ServiceQuoteStatus status) {
        return status == ServiceQuoteStatus.DRAFT
                || status == ServiceQuoteStatus.ISSUED
                || status == ServiceQuoteStatus.ACCEPTED;
    }
}