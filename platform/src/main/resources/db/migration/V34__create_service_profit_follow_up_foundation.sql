CREATE TABLE platform.service_profit_follow_ups (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    opportunity_id UUID NOT NULL,
    owner_principal_id UUID,
    claimed_at TIMESTAMPTZ,
    handling_status VARCHAR(32) NOT NULL,
    next_action_due_at TIMESTAMPTZ,
    current_disposition VARCHAR(40) NOT NULL,
    internal_note VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_service_profit_follow_ups PRIMARY KEY (id),
    CONSTRAINT uq_service_profit_follow_ups_tenant_opportunity
        UNIQUE (tenant_id, opportunity_id),
    CONSTRAINT uq_service_profit_follow_ups_id_tenant
        UNIQUE (id, tenant_id),
    CONSTRAINT fk_service_profit_follow_ups_opportunity
        FOREIGN KEY (opportunity_id, tenant_id)
        REFERENCES platform.service_profit_opportunities(id, tenant_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_service_profit_follow_ups_handling_status
        CHECK (handling_status IN ('OPEN', 'COMPLETED')),
    CONSTRAINT ck_service_profit_follow_ups_disposition
        CHECK (current_disposition IN ('NONE')),
    CONSTRAINT ck_service_profit_follow_ups_note_length
        CHECK (internal_note IS NULL OR char_length(internal_note) <= 500),
    CONSTRAINT ck_service_profit_follow_ups_version
        CHECK (version >= 0)
);

CREATE INDEX ix_service_profit_follow_ups_tenant_opportunity
    ON platform.service_profit_follow_ups(tenant_id, opportunity_id);

CREATE TABLE platform.service_profit_follow_up_history (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    follow_up_id UUID NOT NULL,
    opportunity_id UUID NOT NULL,
    actor_principal_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    previous_value VARCHAR(80),
    new_value VARCHAR(80),
    observed_version BIGINT NOT NULL,
    written_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_service_profit_follow_up_history PRIMARY KEY (id),
    CONSTRAINT fk_service_profit_follow_up_history_follow_up
        FOREIGN KEY (follow_up_id, tenant_id)
        REFERENCES platform.service_profit_follow_ups(id, tenant_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_service_profit_follow_up_history_opportunity
        FOREIGN KEY (opportunity_id, tenant_id)
        REFERENCES platform.service_profit_opportunities(id, tenant_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_service_profit_follow_up_history_event_type
        CHECK (event_type IN (
            'CREATED', 'OWNERSHIP_CLAIMED', 'DUE_DATE_CHANGED',
            'DISPOSITION_CHANGED', 'HANDLING_STATUS_CHANGED'
        )),
    CONSTRAINT ck_service_profit_follow_up_history_versions
        CHECK (observed_version >= 0 AND written_version >= 0)
);

CREATE INDEX ix_service_profit_follow_up_history_tenant_follow_up
    ON platform.service_profit_follow_up_history(tenant_id, follow_up_id, occurred_at, id);