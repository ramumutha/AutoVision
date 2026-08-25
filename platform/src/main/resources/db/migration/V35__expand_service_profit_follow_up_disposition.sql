ALTER TABLE platform.service_profit_follow_ups
    DROP CONSTRAINT ck_service_profit_follow_ups_disposition;

ALTER TABLE platform.service_profit_follow_ups
    ADD CONSTRAINT ck_service_profit_follow_ups_disposition
    CHECK (current_disposition IN (
        'NONE',
        'FOLLOW_UP_REQUIRED',
        'INTEREST_RECORDED',
        'DECLINED_RECORDED',
        'NO_RESPONSE_RECORDED',
        'NO_FURTHER_ACTION'
    ));