package com.autovision.platform.aftersales;

public interface ServiceQuoteLineEligibilityPolicy {

    boolean blocksRequotation(ServiceQuoteStatus status);
}