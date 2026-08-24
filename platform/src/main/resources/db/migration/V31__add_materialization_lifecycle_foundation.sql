-- G6.4A durable dataset materialization lifecycle and replay identity.

ALTER TABLE platform.dataset_processings
    ADD COLUMN materialization_key VARCHAR(255),
    ADD COLUMN materialization_version VARCHAR(80),
    ADD COLUMN materialization_correlation_id UUID,
    ADD COLUMN materialization_started_at TIMESTAMPTZ,
    ADD COLUMN materialized_at TIMESTAMPTZ,
    ADD COLUMN materialization_failed_at TIMESTAMPTZ,
    ADD COLUMN materialization_attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN materialization_failure_reason VARCHAR(1000);

ALTER TABLE platform.dataset_processings
    DROP CONSTRAINT ck_dataset_processings_status;

ALTER TABLE platform.dataset_processings
    ADD CONSTRAINT ck_dataset_processings_status
        CHECK (status IN (
            'RECEIVED', 'STAGED', 'READY_FOR_MATERIALIZATION',
            'MATERIALIZING', 'MATERIALIZED', 'MATERIALIZATION_FAILED',
            'VALIDATION_FAILED', 'QUARANTINED'
        )),
    ADD CONSTRAINT ck_dataset_processings_materialization_identity
        CHECK ((materialization_key IS NULL AND materialization_version IS NULL)
            OR (materialization_key IS NOT NULL AND materialization_version IS NOT NULL)),
    ADD CONSTRAINT ck_dataset_processings_materialization_attempts
        CHECK (materialization_attempt_count >= 0),
    ADD CONSTRAINT ck_dataset_processings_materialization_timestamps
        CHECK (materialized_at IS NULL OR materialization_started_at IS NOT NULL),
    ADD CONSTRAINT ck_dataset_processings_materialization_failure
        CHECK (materialization_failed_at IS NULL OR materialization_failure_reason IS NOT NULL),
    ADD CONSTRAINT ck_dataset_processings_materialized_state
        CHECK (status <> 'MATERIALIZED' OR materialized_at IS NOT NULL),
    ADD CONSTRAINT ck_dataset_processings_materializing_state
        CHECK (status NOT IN ('MATERIALIZING', 'MATERIALIZATION_FAILED')
            OR materialization_key IS NOT NULL);

CREATE INDEX ix_dataset_processings_materialization_identity
    ON platform.dataset_processings (
        tenant_id, dataset_id, dataset_version,
        materialization_key, materialization_version
    );