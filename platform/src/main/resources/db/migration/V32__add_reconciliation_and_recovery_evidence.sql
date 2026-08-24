-- G6.5A durable reconciliation, rejection, failure, and operational evidence.

ALTER TABLE platform.dataset_processings
    ADD COLUMN materialization_failure_stage VARCHAR(40),
    ADD COLUMN materialization_failure_code VARCHAR(100),
    ADD COLUMN materialization_failure_attempt INTEGER,
    ADD COLUMN materialization_failure_replayable BOOLEAN;

ALTER TABLE platform.dataset_processings
    ADD CONSTRAINT ck_dataset_processings_failure_metadata
        CHECK ((materialization_failure_stage IS NULL
                AND materialization_failure_code IS NULL
                AND materialization_failure_attempt IS NULL
                AND materialization_failure_replayable IS NULL)
            OR (materialization_failure_stage IS NOT NULL
                AND materialization_failure_code IS NOT NULL
                AND materialization_failure_attempt IS NOT NULL
                AND materialization_failure_attempt > 0
                AND materialization_failure_replayable IS NOT NULL)),
    ADD CONSTRAINT ck_dataset_processings_failure_stage
        CHECK (materialization_failure_stage IS NULL OR length(trim(materialization_failure_stage)) > 0),
    ADD CONSTRAINT ck_dataset_processings_failure_code
        CHECK (materialization_failure_code IS NULL OR length(trim(materialization_failure_code)) > 0);

CREATE TABLE platform.dataset_reconciliation_summaries (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    dataset_processing_id UUID NOT NULL,
    dataset_id VARCHAR(160) NOT NULL,
    dataset_version VARCHAR(80) NOT NULL,
    processing_correlation_id UUID NOT NULL,
    records_received INTEGER NOT NULL DEFAULT 0,
    records_parsed INTEGER NOT NULL DEFAULT 0,
    records_staged INTEGER NOT NULL DEFAULT 0,
    records_quarantined INTEGER NOT NULL DEFAULT 0,
    records_excluded INTEGER NOT NULL DEFAULT 0,
    records_eligible INTEGER NOT NULL DEFAULT 0,
    records_mapped INTEGER NOT NULL DEFAULT 0,
    opportunities_detected INTEGER NOT NULL DEFAULT 0,
    opportunities_persisted INTEGER NOT NULL DEFAULT 0,
    duplicate_no_op_count INTEGER NOT NULL DEFAULT 0,
    fatal_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    warning_count INTEGER NOT NULL DEFAULT 0,
    info_count INTEGER NOT NULL DEFAULT 0,
    materialization_key VARCHAR(255),
    materialization_version VARCHAR(80),
    status VARCHAR(40) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_dataset_reconciliation_summaries PRIMARY KEY (id),
    CONSTRAINT fk_dataset_reconciliation_summaries_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.tenants(id),
    CONSTRAINT uq_dataset_reconciliation_summaries_dataset UNIQUE (tenant_id, dataset_processing_id),
    CONSTRAINT fk_dataset_reconciliation_summaries_dataset
        FOREIGN KEY (dataset_processing_id, tenant_id)
        REFERENCES platform.dataset_processings(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT ck_dataset_reconciliation_summaries_counts CHECK (
        records_received >= 0 AND records_parsed >= 0 AND records_staged >= 0
        AND records_quarantined >= 0 AND records_excluded >= 0 AND records_eligible >= 0
        AND records_mapped >= 0 AND opportunities_detected >= 0 AND opportunities_persisted >= 0
        AND duplicate_no_op_count >= 0 AND fatal_count >= 0 AND error_count >= 0
        AND warning_count >= 0 AND info_count >= 0
        AND records_parsed <= records_received
        AND records_staged + records_quarantined <= records_parsed
        AND records_eligible <= records_staged
        AND opportunities_persisted <= opportunities_detected
    ),
    CONSTRAINT ck_dataset_reconciliation_summaries_materialization_identity CHECK (
        (materialization_key IS NULL AND materialization_version IS NULL)
        OR (materialization_key IS NOT NULL AND materialization_version IS NOT NULL)
    )
);

CREATE TABLE platform.pre_staging_rejections (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    dataset_processing_id UUID NOT NULL,
    source_record_type VARCHAR(40),
    safe_source_record_id VARCHAR(160),
    validation_stage VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    rejection_code VARCHAR(100) NOT NULL,
    safe_field_path VARCHAR(255),
    processing_correlation_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_pre_staging_rejections PRIMARY KEY (id),
    CONSTRAINT fk_pre_staging_rejections_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.tenants(id),
    CONSTRAINT fk_pre_staging_rejections_dataset
        FOREIGN KEY (dataset_processing_id, tenant_id)
        REFERENCES platform.dataset_processings(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT ck_pre_staging_rejections_type CHECK (
        source_record_type IS NULL OR source_record_type IN (
            'ORGANIZATION', 'CUSTOMER', 'VEHICLE', 'SERVICE_ORDER', 'SERVICE_JOB',
            'SERVICE_LINE', 'RECOMMENDATION', 'DISPOSITION', 'NOTE', 'SERVICE_HISTORY',
            'INVOICE', 'INVOICE_LINE', 'COST'
        )
    ),
    CONSTRAINT ck_pre_staging_rejections_severity CHECK (severity IN ('FATAL', 'ERROR', 'WARNING', 'INFO')),
    CONSTRAINT ck_pre_staging_rejections_text CHECK (length(trim(rejection_code)) > 0)
);

CREATE INDEX ix_pre_staging_rejections_dataset
    ON platform.pre_staging_rejections (tenant_id, dataset_processing_id, occurred_at);

CREATE TABLE platform.dataset_operational_events (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    dataset_processing_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    materialization_key VARCHAR(255),
    materialization_version VARCHAR(80),
    materialization_attempt INTEGER,
    actor_principal_id UUID,
    application_id VARCHAR(120),
    processing_correlation_id UUID NOT NULL,
    stable_code VARCHAR(100),
    safe_reason VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_dataset_operational_events PRIMARY KEY (id),
    CONSTRAINT fk_dataset_operational_events_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.tenants(id),
    CONSTRAINT fk_dataset_operational_events_dataset
        FOREIGN KEY (dataset_processing_id, tenant_id)
        REFERENCES platform.dataset_processings(id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT ck_dataset_operational_events_actor CHECK (
        actor_principal_id IS NOT NULL OR application_id IS NOT NULL
    ),
    CONSTRAINT ck_dataset_operational_events_attempt CHECK (
        materialization_attempt IS NULL OR materialization_attempt > 0
    ),
    CONSTRAINT ck_dataset_operational_events_identity CHECK (
        (materialization_key IS NULL AND materialization_version IS NULL)
        OR (materialization_key IS NOT NULL AND materialization_version IS NOT NULL)
    )
);

CREATE INDEX ix_dataset_operational_events_dataset
    ON platform.dataset_operational_events (tenant_id, dataset_processing_id, occurred_at, id);
