-- Durable source-record provenance for Service Profit opportunities.
-- This table stores identifiers and classification only; raw payload and PII
-- remain outside the Service Profit evidence contract.

CREATE TABLE platform.service_profit_opportunity_evidence (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    opportunity_id UUID NOT NULL,

    evidence_source_type VARCHAR(40) NOT NULL,
    source_system VARCHAR(80) NOT NULL,
    source_record_id VARCHAR(200) NOT NULL,
    source_parent_record_id VARCHAR(200),
    evidence_classification VARCHAR(32) NOT NULL,
    evidence_strength VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_service_profit_opportunity_evidence
        PRIMARY KEY (id),

    CONSTRAINT uq_service_profit_opportunity_evidence_identity
        UNIQUE (
            tenant_id,
            opportunity_id,
            evidence_source_type,
            source_system,
            source_record_id
        ),

    CONSTRAINT uq_service_profit_opportunity_evidence_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT fk_service_profit_opportunity_evidence_opportunity
        FOREIGN KEY (opportunity_id, tenant_id)
        REFERENCES platform.service_profit_opportunities(id, tenant_id)
        ON DELETE CASCADE,

    CONSTRAINT ck_service_profit_opportunity_evidence_source_type
        CHECK (evidence_source_type IN (
            'REPAIR_ORDER',
            'SERVICE_JOB',
            'RECOMMENDATION',
            'DISPOSITION',
            'ADVISOR_NOTE',
            'SERVICE_HISTORY',
            'INVOICE',
            'INVOICE_LINE',
            'COST_RECORD',
            'CUSTOMER_RECORD',
            'VEHICLE_RECORD'
        )),

    CONSTRAINT ck_service_profit_opportunity_evidence_source_system
        CHECK (btrim(source_system) <> ''),

    CONSTRAINT ck_service_profit_opportunity_evidence_source_record_id
        CHECK (btrim(source_record_id) <> ''),

    CONSTRAINT ck_service_profit_opportunity_evidence_classification
        CHECK (evidence_classification IN (
            'SOURCE_CONFIRMED',
            'EVIDENCE_DERIVED',
            'POLICY_DERIVED'
        )),

    CONSTRAINT ck_service_profit_opportunity_evidence_strength
        CHECK (evidence_strength IN (
            'STRONG',
            'MODERATE',
            'WEAK'
        ))
);

CREATE INDEX ix_service_profit_opportunity_evidence_tenant_opportunity
    ON platform.service_profit_opportunity_evidence(
        tenant_id,
        opportunity_id,
        created_at
    );

CREATE INDEX ix_service_profit_opportunity_evidence_tenant_source
    ON platform.service_profit_opportunity_evidence(
        tenant_id,
        evidence_source_type,
        source_record_id
    );