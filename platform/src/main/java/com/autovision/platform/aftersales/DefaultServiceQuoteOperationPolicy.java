package com.autovision.platform.aftersales;

import org.springframework.stereotype.Component;

@Component
public class DefaultServiceQuoteOperationPolicy
        implements ServiceQuoteOperationPolicy {

    @Override
    public boolean isAllowed(
            ServiceQuoteStatus currentStatus,
            ServiceQuoteOperation operation
    ) {
        return currentStatus != null && operation != null;
    }
}