-- Sprint 5 S5.3.3 Service Line persistence foundation.
--
-- Establishes ServiceLine as the transactional work/material line within
-- the ServiceOrder aggregate.
--
-- Every ServiceLine belongs to exactly one ServiceOrder.
--
-- service_job_id is optional:
--
--   ServiceOrder -> ServiceLine
--
-- supports simple, urgent and workflow-light service activity.
--
--   ServiceOrder -> ServiceJob -> ServiceLine
--
-- supports normal workshop execution where work is grouped into an
-- executable ServiceJob.
--
-- When service_job_id is present, the composite foreign key guarantees
-- that the referenced ServiceJob belongs to the same ServiceOrder.
--
-- ServiceLine deliberately carries no independent workflow status.
-- Workshop execution status belongs to ServiceOrder and/or ServiceJob.
--
-- Pricing, tax, discounts, invoicing, payment, inventory reservation,
-- stock movement, technician assignment, checkpoints, instructions,
-- Inspection, AI/Finding and CustomerAuthorization linkage are excluded.
--
-- ServiceLine must remain distinct from future QuoteLine, SalesLine,
-- PurchaseLine and InvoiceLine concepts.

CREATE TABLE platform.service_lines (
    id UUID PRIMARY KEY,

    service_order_id UUID NOT NULL,
    service_job_id UUID,

    line_number INTEGER NOT NULL,
    line_type VARCHAR(32) NOT NULL,

    description VARCHAR(500) NOT NULL,

    quantity NUMERIC(19, 4) NOT NULL,
    unit_of_measure VARCHAR(32) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_service_lines_service_order
        FOREIGN KEY (service_order_id)
        REFERENCES platform.service_orders(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_lines_service_job
        FOREIGN KEY (
            service_job_id,
            service_order_id
        )
        REFERENCES platform.service_jobs(
            id,
            service_order_id
        )
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_lines_created_by
        FOREIGN KEY (created_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_lines_updated_by
        FOREIGN KEY (updated_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_lines_order_line_number
        UNIQUE (service_order_id, line_number),

    CONSTRAINT ck_service_lines_line_number
        CHECK (line_number > 0),

    CONSTRAINT ck_service_lines_description
        CHECK (length(trim(description)) > 0),

    CONSTRAINT ck_service_lines_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_service_lines_unit_of_measure
        CHECK (length(trim(unit_of_measure)) > 0),

    CONSTRAINT ck_service_lines_version
        CHECK (version >= 0)
);

CREATE INDEX ix_service_lines_service_order_id
    ON platform.service_lines(service_order_id);

CREATE INDEX ix_service_lines_service_job_id
    ON platform.service_lines(service_job_id);

CREATE INDEX ix_service_lines_order_job
    ON platform.service_lines(
        service_order_id,
        service_job_id
    );

CREATE INDEX ix_service_lines_order_line_type
    ON platform.service_lines(
        service_order_id,
        line_type
    );