-- G6.2 provider-neutral dataset identity, source lineage, findings, and quarantine.
-- Parsing, mapping, materialization, and DELTA processing remain future slices.

CREATE TABLE platform.dataset_processings (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    dealer_id UUID,
    location_id UUID,
    dataset_id VARCHAR(160) NOT NULL,
    dataset_version VARCHAR(80) NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    source_provider VARCHAR(100),
    source_schema_version VARCHAR(100) NOT NULL,
    delivery_type VARCHAR(20) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    checksum_algorithm VARCHAR(30),
    content_checksum VARCHAR(128),
    content_byte_size BIGINT,
    mapping_version VARCHAR(100),
    processing_correlation_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_dataset_processings PRIMARY KEY (id),
    CONSTRAINT uq_dataset_processings_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_dataset_processings_identity
        UNIQUE (tenant_id, dataset_id, dataset_version),
    CONSTRAINT fk_dataset_processings_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.tenants(id),
    CONSTRAINT fk_dataset_processings_dealer
        FOREIGN KEY (tenant_id, dealer_id)
        REFERENCES platform.dealers(tenant_id, id),
    CONSTRAINT fk_dataset_processings_location
        FOREIGN KEY (tenant_id, location_id)
        REFERENCES platform.locations(tenant_id, id),
    CONSTRAINT ck_dataset_processings_effective_window
        CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from),
    CONSTRAINT ck_dataset_processings_delivery_type
        CHECK (delivery_type IN ('FULL')),
    CONSTRAINT ck_dataset_processings_status
        CHECK (status IN ('RECEIVED', 'STAGED', 'VALIDATION_FAILED', 'QUARANTINED')),
    CONSTRAINT ck_dataset_processings_checksum_pair
        CHECK ((checksum_algorithm IS NULL AND content_checksum IS NULL)
            OR (checksum_algorithm IS NOT NULL AND content_checksum IS NOT NULL)),
    CONSTRAINT ck_dataset_processings_content_byte_size
        CHECK (content_byte_size IS NULL OR content_byte_size >= 0)
);

CREATE INDEX ix_dataset_processings_tenant_status
    ON platform.dataset_processings (tenant_id, status, received_at DESC);

CREATE INDEX ix_dataset_processings_tenant_dealer
    ON platform.dataset_processings (tenant_id, dealer_id, received_at DESC);

CREATE TABLE platform.staged_source_records (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    dataset_processing_id UUID NOT NULL,
    record_type VARCHAR(40) NOT NULL,
    source_record_id VARCHAR(160) NOT NULL,
    source_parent_id VARCHAR(160),
    source_version VARCHAR(80),
    source_hash VARCHAR(128),
    raw_payload JSONB,
    validation_status VARCHAR(20) NOT NULL,
    state VARCHAR(20) NOT NULL,
    materialization_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_staged_source_records PRIMARY KEY (id),
    CONSTRAINT uq_staged_source_records_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_staged_source_records_identity
        UNIQUE (tenant_id, dataset_processing_id, record_type, source_record_id),
    CONSTRAINT fk_staged_source_records_dataset
        FOREIGN KEY (dataset_processing_id, tenant_id)
        REFERENCES platform.dataset_processings(id, tenant_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_staged_source_records_type
        CHECK (record_type IN (
            'ORGANIZATION', 'CUSTOMER', 'VEHICLE', 'SERVICE_ORDER',
            'SERVICE_JOB', 'SERVICE_LINE', 'RECOMMENDATION', 'DISPOSITION',
            'NOTE', 'SERVICE_HISTORY', 'INVOICE', 'INVOICE_LINE', 'COST'
        )),
    CONSTRAINT ck_staged_source_records_validation_status
        CHECK (validation_status IN ('PENDING', 'PASSED', 'FAILED')),
    CONSTRAINT ck_staged_source_records_state
        CHECK (state IN ('STAGED', 'QUARANTINED', 'REJECTED')),
    CONSTRAINT ck_staged_source_records_identity
        CHECK (length(trim(source_record_id)) > 0),
    CONSTRAINT ck_staged_source_records_payload
        CHECK (raw_payload IS NULL OR jsonb_typeof(raw_payload) = 'object'),
    CONSTRAINT ck_staged_source_records_materialization
        CHECK (state <> 'QUARANTINED' OR materialization_eligible = FALSE)
);

CREATE INDEX ix_staged_source_records_tenant_dataset
    ON platform.staged_source_records (tenant_id, dataset_processing_id, created_at);

CREATE INDEX ix_staged_source_records_tenant_state
    ON platform.staged_source_records (tenant_id, state, created_at);

CREATE TABLE platform.validation_findings (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    dataset_processing_id UUID NOT NULL,
    staged_source_record_id UUID,
    validation_stage VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    finding_code VARCHAR(100) NOT NULL,
    field_path VARCHAR(255),
    safe_message VARCHAR(1000) NOT NULL,
    capability_affected VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_validation_findings PRIMARY KEY (id),
    CONSTRAINT fk_validation_findings_dataset
        FOREIGN KEY (dataset_processing_id, tenant_id)
        REFERENCES platform.dataset_processings(id, tenant_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_validation_findings_record
        FOREIGN KEY (staged_source_record_id, tenant_id)
        REFERENCES platform.staged_source_records(id, tenant_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_validation_findings_stage
        CHECK (validation_stage IN (
            'ENVELOPE', 'SCHEMA', 'TYPE_FORMAT', 'REFERENTIAL_INTEGRITY',
            'CONTAINMENT', 'BUSINESS_SEMANTIC', 'CAPABILITY_READINESS',
            'MATERIALIZATION_ELIGIBILITY'
        )),
    CONSTRAINT ck_validation_findings_severity
        CHECK (severity IN ('FATAL', 'ERROR', 'WARNING', 'INFO')),
    CONSTRAINT ck_validation_findings_code
        CHECK (length(trim(finding_code)) > 0),
    CONSTRAINT ck_validation_findings_message
        CHECK (length(trim(safe_message)) > 0)
);

CREATE INDEX ix_validation_findings_tenant_dataset
    ON platform.validation_findings (tenant_id, dataset_processing_id, created_at);

CREATE INDEX ix_validation_findings_tenant_record
    ON platform.validation_findings (tenant_id, staged_source_record_id, created_at);