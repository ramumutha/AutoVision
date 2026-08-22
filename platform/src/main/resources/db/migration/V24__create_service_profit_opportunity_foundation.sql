-- AutoVision Service Profit AI R1.2.1 opportunity persistence foundation.
-- Opportunities represent commercial recovery intelligence and do not replace
-- authoritative ServiceOrder, ServiceJob, ServiceLine or ServiceQuote records.

CREATE TABLE platform.service_profit_opportunities (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,

    dealer_id UUID,
    branch_id UUID,
    location_id UUID,

    customer_id UUID,
    vehicle_id UUID,

    opportunity_key VARCHAR(200) NOT NULL,

    opportunity_type VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,

    evidence_class VARCHAR(32) NOT NULL,
    evidence_strength VARCHAR(32) NOT NULL,

    priority VARCHAR(16) NOT NULL,
    actionability VARCHAR(40) NOT NULL,

    title VARCHAR(200) NOT NULL,
    summary VARCHAR(1000),

    potential_amount DECIMAL(19,4),
    currency_code VARCHAR(3),

    source_system VARCHAR(80) NOT NULL,
    source_entity_type VARCHAR(80) NOT NULL,
    source_entity_id VARCHAR(200) NOT NULL,

    source_service_order_id UUID,
    source_service_job_id UUID,
    source_service_line_id UUID,
    source_quote_id UUID,

    policy_version VARCHAR(80) NOT NULL,

    detected_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_by_principal_id UUID,
    updated_by_principal_id UUID,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_service_profit_opportunities
        PRIMARY KEY (id),

    CONSTRAINT uq_service_profit_opportunities_tenant_key
        UNIQUE (tenant_id, opportunity_key),

    CONSTRAINT uq_service_profit_opportunities_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT ck_service_profit_opportunities_key_not_blank
        CHECK (btrim(opportunity_key) <> ''),

    CONSTRAINT ck_service_profit_opportunities_type
        CHECK (opportunity_type IN (
            'DECLINED_WORK',
            'DEFERRED_WORK',
            'DUE_SERVICE',
            'OVERDUE_SERVICE',
            'INACTIVE_CUSTOMER'
        )),

    CONSTRAINT ck_service_profit_opportunities_status
        CHECK (status IN (
            'DETECTED',
            'QUALIFIED',
            'ASSIGNED',
            'CONTACTED',
            'CLOSED',
            'REJECTED',
            'INVALID',
            'DUPLICATE',
            'SUPPRESSED',
            'EXPIRED'
        )),

    CONSTRAINT ck_service_profit_opportunities_evidence_class
        CHECK (evidence_class IN (
            'SOURCE_CONFIRMED',
            'EVIDENCE_DERIVED',
            'POLICY_DERIVED'
        )),

    CONSTRAINT ck_service_profit_opportunities_evidence_strength
        CHECK (evidence_strength IN (
            'STRONG',
            'MODERATE',
            'WEAK'
        )),

    CONSTRAINT ck_service_profit_opportunities_priority
        CHECK (priority IN (
            'HIGH',
            'MEDIUM',
            'LOW'
        )),

    CONSTRAINT ck_service_profit_opportunities_actionability
        CHECK (actionability IN (
            'READY',
            'REVIEW_REQUIRED',
            'CONTACT_DATA_MISSING',
            'BLOCKED',
            'SUPPRESSED'
        )),

    CONSTRAINT ck_service_profit_opportunities_title_not_blank
        CHECK (btrim(title) <> ''),

    CONSTRAINT ck_service_profit_opportunities_source_system_not_blank
        CHECK (btrim(source_system) <> ''),

    CONSTRAINT ck_service_profit_opportunities_source_entity_type_not_blank
        CHECK (btrim(source_entity_type) <> ''),

    CONSTRAINT ck_service_profit_opportunities_source_entity_id_not_blank
        CHECK (btrim(source_entity_id) <> ''),

    CONSTRAINT ck_service_profit_opportunities_policy_version_not_blank
        CHECK (btrim(policy_version) <> ''),

    CONSTRAINT ck_service_profit_opportunities_potential_amount
        CHECK (
            potential_amount IS NULL
            OR potential_amount >= 0
        ),

    CONSTRAINT ck_service_profit_opportunities_potential_value_pair
        CHECK (
            (
                potential_amount IS NULL
                AND currency_code IS NULL
            )
            OR
            (
                potential_amount IS NOT NULL
                AND currency_code IS NOT NULL
            )
        )
);

CREATE INDEX ix_service_profit_opportunities_tenant_status
    ON platform.service_profit_opportunities(
        tenant_id,
        status
    );

CREATE INDEX ix_service_profit_opportunities_tenant_dealer_status
    ON platform.service_profit_opportunities(
        tenant_id,
        dealer_id,
        status
    );

CREATE INDEX ix_service_profit_opportunities_tenant_branch_status
    ON platform.service_profit_opportunities(
        tenant_id,
        branch_id,
        status
    );

CREATE INDEX ix_service_profit_opportunities_tenant_location_status
    ON platform.service_profit_opportunities(
        tenant_id,
        location_id,
        status
    );

CREATE INDEX ix_service_profit_opportunities_tenant_priority_status
    ON platform.service_profit_opportunities(
        tenant_id,
        priority,
        status
    );

CREATE INDEX ix_service_profit_opportunities_tenant_detected
    ON platform.service_profit_opportunities(
        tenant_id,
        detected_at,
        id
    );
