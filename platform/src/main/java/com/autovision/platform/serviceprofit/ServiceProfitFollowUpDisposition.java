package com.autovision.platform.serviceprofit;

public enum ServiceProfitFollowUpDisposition {
    /** No AutoVision internal disposition has been recorded yet. */
    NONE,

    /** Internal handling still requires another action; this is not authorization. */
    FOLLOW_UP_REQUIRED,

    /** An operational user recorded customer interest; this is not authorization. */
    INTEREST_RECORDED,

    /** An operational user recorded a follow-up decline; this is not DMS evidence. */
    DECLINED_RECORDED,

    /** An operational user recorded no response to a follow-up attempt. */
    NO_RESPONSE_RECORDED,

    /** Dealer internal decision that no further follow-up is currently required. */
    NO_FURTHER_ACTION
}