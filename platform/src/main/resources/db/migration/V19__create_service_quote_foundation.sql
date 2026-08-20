-- Sprint 5 S5.5.1-B Quote / Estimate persistence foundation.
-- Quotes remain optional and multiple quotes may belong to one ServiceOrder.

ALTER TABLE platform.aftersales_cases
    ADD CONSTRAINT uq_aftersales_cases_id_tenant_dealer_branch
        UNIQUE (id, tenant_id, dealer_id, branch_id);

ALTER TABLE platform.service_orders
    ADD CONSTRAINT uq_service_orders_id_tenant_dealer_branch
        UNIQUE (id, tenant_id, dealer_id, branch_id);

CREATE TABLE platform.service_quotes (
    id UUID PRIMARY KEY,

    tenant_id UUID NOT NULL,
    dealer_id UUID,
    branch_id UUID,

    after_sales_case_id UUID NOT NULL,
    service_order_id UUID NOT NULL,

    quote_number VARCHAR(80) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,

    valid_until TIMESTAMPTZ,
    terms_snapshot VARCHAR(4000),
    disclaimer_snapshot VARCHAR(4000),

    issued_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    declined_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    superseded_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    created_by_principal_id UUID NOT NULL,
    updated_by_principal_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_service_quotes_after_sales_case
        FOREIGN KEY (after_sales_case_id, tenant_id)
        REFERENCES platform.aftersales_cases(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_quotes_service_order
        FOREIGN KEY (service_order_id, tenant_id)
        REFERENCES platform.service_orders(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_quotes_dealer
        FOREIGN KEY (dealer_id, tenant_id)
        REFERENCES platform.dealers(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_quotes_branch
        FOREIGN KEY (branch_id, tenant_id, dealer_id)
        REFERENCES platform.branches(id, tenant_id, dealer_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_quotes_tenant_quote_number
        UNIQUE (tenant_id, quote_number),

    CONSTRAINT uq_service_quotes_id_service_order
        UNIQUE (id, service_order_id),

    CONSTRAINT uq_service_quotes_id_scope
        UNIQUE (id, tenant_id, after_sales_case_id, service_order_id),

    CONSTRAINT ck_service_quotes_branch_requires_dealer
        CHECK (branch_id IS NULL OR dealer_id IS NOT NULL),

    CONSTRAINT ck_service_quotes_status
        CHECK (status IN (
            'DRAFT', 'ISSUED', 'ACCEPTED', 'DECLINED', 'CANCELLED',
            'EXPIRED', 'SUPERSEDED'
        )),

    CONSTRAINT ck_service_quotes_version
        CHECK (version >= 0)
);

CREATE TABLE platform.service_quote_lines (
    id UUID PRIMARY KEY,

    service_quote_id UUID NOT NULL,
    service_line_id UUID NOT NULL,
    service_job_id UUID,

    description_snapshot VARCHAR(500) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    net_amount NUMERIC(19, 4) NOT NULL,
    tax_amount NUMERIC(19, 4) NOT NULL,
    gross_amount NUMERIC(19, 4) NOT NULL,
    sequence INTEGER NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_by_principal_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_service_quote_lines_quote
        FOREIGN KEY (service_quote_id)
        REFERENCES platform.service_quotes(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_quote_lines_service_line
        FOREIGN KEY (service_line_id)
        REFERENCES platform.service_lines(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_quote_lines_service_job
        FOREIGN KEY (service_job_id)
        REFERENCES platform.service_jobs(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_quote_lines_quote_service_line
        UNIQUE (service_quote_id, service_line_id),

    CONSTRAINT ck_service_quote_lines_quantity
        CHECK (quantity > 0),
    CONSTRAINT ck_service_quote_lines_unit_price
        CHECK (unit_price >= 0),
    CONSTRAINT ck_service_quote_lines_net_amount
        CHECK (net_amount >= 0),
    CONSTRAINT ck_service_quote_lines_tax_amount
        CHECK (tax_amount >= 0),
    CONSTRAINT ck_service_quote_lines_gross_amount
        CHECK (gross_amount >= 0),
    CONSTRAINT ck_service_quote_lines_sequence
        CHECK (sequence >= 0),
    CONSTRAINT ck_service_quote_lines_version
        CHECK (version >= 0)
);

CREATE INDEX ix_service_quotes_service_order_id
    ON platform.service_quotes(service_order_id);
CREATE INDEX ix_service_quotes_after_sales_case_id
    ON platform.service_quotes(after_sales_case_id);
CREATE INDEX ix_service_quotes_tenant_status
    ON platform.service_quotes(tenant_id, status);

CREATE INDEX ix_service_quote_lines_service_quote_id
    ON platform.service_quote_lines(service_quote_id);
CREATE INDEX ix_service_quote_lines_service_line_id
    ON platform.service_quote_lines(service_line_id);
CREATE INDEX ix_service_quote_lines_service_job_id
    ON platform.service_quote_lines(service_job_id)
    WHERE service_job_id IS NOT NULL;

CREATE OR REPLACE FUNCTION platform.validate_service_quote_line_provenance()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    quote_order_id UUID;
    line_order_id UUID;
    line_job_id UUID;
    job_order_id UUID;
BEGIN
    SELECT service_order_id
      INTO quote_order_id
      FROM platform.service_quotes
     WHERE id = NEW.service_quote_id;

    SELECT service_order_id, service_job_id
      INTO line_order_id, line_job_id
      FROM platform.service_lines
     WHERE id = NEW.service_line_id;

    IF quote_order_id IS DISTINCT FROM line_order_id THEN
        RAISE EXCEPTION 'ServiceQuoteLine ServiceLine must belong to the Quote ServiceOrder';
    END IF;

    IF NEW.service_job_id IS NOT NULL THEN
        SELECT service_order_id
          INTO job_order_id
          FROM platform.service_jobs
         WHERE id = NEW.service_job_id;

        IF quote_order_id IS DISTINCT FROM job_order_id THEN
            RAISE EXCEPTION 'ServiceQuoteLine ServiceJob must belong to the Quote ServiceOrder';
        END IF;

        IF line_job_id IS NOT NULL
           AND NEW.service_job_id IS DISTINCT FROM line_job_id THEN
            RAISE EXCEPTION 'ServiceQuoteLine ServiceJob must match the ServiceLine ServiceJob';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_service_quote_line_provenance
    BEFORE INSERT OR UPDATE ON platform.service_quote_lines
    FOR EACH ROW
    EXECUTE FUNCTION platform.validate_service_quote_line_provenance();