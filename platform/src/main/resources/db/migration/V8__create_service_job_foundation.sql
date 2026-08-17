-- Sprint 5 S5.3.2 Service Job persistence foundation.
--
-- Establishes ServiceJob as an optional executable work package within
-- the ServiceOrder aggregate.
--
-- A ServiceOrder may contain ServiceJobs but is not required to do so.
-- Simple, urgent or workflow-light work must remain capable of being
-- represented directly by future ServiceLines under the ServiceOrder.
--
-- ServiceJob owns workshop execution state and operational approval
-- readiness. Job approval is deliberately separate from the authoritative
-- CustomerAuthorization business record because future approval evidence
-- may originate from customer, warranty, insurance, fleet, internal or
-- other configured approval mechanisms.
--
-- Technician assignment, Job checkpoints, Job instructions, time entries,
-- evidence capture, parts reservation, pricing, ServiceLine, Inspection,
-- AI/Finding, Quote, Appointment and Invoice integration are deliberately
-- excluded from this migration.
--
-- uq_service_jobs_id_service_order is intentionally provided so a future
-- ServiceLine can enforce that an optional service_job_id belongs to the
-- same service_order_id.

CREATE TABLE platform.service_jobs (
    id UUID PRIMARY KEY,

    service_order_id UUID NOT NULL,

    job_number VARCHAR(80) NOT NULL,
    summary VARCHAR(500) NOT NULL,

    status VARCHAR(32) NOT NULL,
    approval_status VARCHAR(32) NOT NULL,

    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ready_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,

    approved_at TIMESTAMPTZ,
    approved_by_principal_id UUID,

    version BIGINT NOT NULL DEFAULT 0,

    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_service_jobs_service_order
        FOREIGN KEY (service_order_id)
        REFERENCES platform.service_orders(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_jobs_approved_by
        FOREIGN KEY (approved_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_jobs_created_by
        FOREIGN KEY (created_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_jobs_updated_by
        FOREIGN KEY (updated_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_jobs_order_job_number
        UNIQUE (service_order_id, job_number),

    CONSTRAINT uq_service_jobs_id_service_order
        UNIQUE (id, service_order_id),

    CONSTRAINT ck_service_jobs_job_number
        CHECK (length(trim(job_number)) > 0),

    CONSTRAINT ck_service_jobs_summary
        CHECK (length(trim(summary)) > 0),

    CONSTRAINT ck_service_jobs_ready_at
        CHECK (
            ready_at IS NULL
            OR ready_at >= opened_at
        ),

    CONSTRAINT ck_service_jobs_started_at
        CHECK (
            started_at IS NULL
            OR started_at >= opened_at
        ),

    CONSTRAINT ck_service_jobs_completed_at
        CHECK (
            completed_at IS NULL
            OR completed_at >= opened_at
        ),

    CONSTRAINT ck_service_jobs_cancelled_at
        CHECK (
            cancelled_at IS NULL
            OR cancelled_at >= opened_at
        ),

    CONSTRAINT ck_service_jobs_approved_at
        CHECK (
            approved_at IS NULL
            OR approved_at >= opened_at
        ),

    CONSTRAINT ck_service_jobs_version
        CHECK (version >= 0)
);

CREATE INDEX ix_service_jobs_service_order_id
    ON platform.service_jobs(service_order_id);

CREATE INDEX ix_service_jobs_service_order_status
    ON platform.service_jobs(
        service_order_id,
        status
    );

CREATE INDEX ix_service_jobs_service_order_approval_status
    ON platform.service_jobs(
        service_order_id,
        approval_status
    );

CREATE INDEX ix_service_jobs_opened_at
    ON platform.service_jobs(opened_at);