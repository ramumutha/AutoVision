package com.autovision.platform.aftersales;

public interface ServiceQuoteOperationPolicy {

    boolean isAllowed(
            ServiceQuoteStatus currentStatus,
            ServiceQuoteOperation operation
    );
}