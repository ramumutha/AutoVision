ALTER TABLE platform.service_profit_opportunities
    ADD COLUMN suppression_reason VARCHAR(64);

ALTER TABLE platform.service_profit_opportunities
    ADD COLUMN suppressed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE platform.service_profit_opportunities
    ADD CONSTRAINT ck_service_profit_opportunity_suppression
    CHECK (
        (status = 'SUPPRESSED'
            AND suppression_reason IS NOT NULL
            AND suppressed_at IS NOT NULL)
        OR
        (status <> 'SUPPRESSED'
            AND suppression_reason IS NULL
            AND suppressed_at IS NULL)
    );
