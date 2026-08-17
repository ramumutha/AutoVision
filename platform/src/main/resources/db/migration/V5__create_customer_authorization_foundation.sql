-- Sprint 5 Customer Authorization persistence foundation.
--
-- Establishes an auditable customer authorization business record associated
-- with the AfterSalesCase lifecycle.
--
-- Inspection and AI capabilities are optional add-ons and are never
-- prerequisites for customer authorization.
--
-- Quote, Appointment, ServiceOrder, OrderLine and Invoice remain separate
-- Core DMS transactional concepts. This migration deliberately introduces
-- no dependency on those future/independent aggregates.
--
-- Authorization/commercial snapshots preserve what was presented to the
-- customer at authorization time. They are historical snapshots only and
-- must not become substitutes for authoritative transactional structures.

CREATE TABLE platform.customer_authorizations (
    id UUID PRIMARY KEY,

    tenant_id UUID NOT NULL,
    dealer_id UUID,
    branch_id UUID,

    aftersales_case_id UUID NOT NULL,

    authorization_number VARCHAR(80) NOT NULL,
    authorization_status VARCHAR(32) NOT NULL,

    customer_reference VARCHAR(160),
    customer_display_name_snapshot VARCHAR(200),

    authorization_summary VARCHAR(500) NOT NULL,

    authorization_scope_snapshot JSONB NOT NULL,
    commercial_snapshot JSONB,

    terms_snapshot TEXT,
    disclaimer_snapshot TEXT,

    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,

    decision_channel VARCHAR(32),
    decision_reference VARCHAR(160),

    version BIGINT NOT NULL DEFAULT 0,

    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_customer_authorizations_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_customer_authorizations_dealer
        FOREIGN KEY (dealer_id, tenant_id)
        REFERENCES platform.dealers(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_customer_authorizations_branch
        FOREIGN KEY (branch_id, tenant_id, dealer_id)
        REFERENCES platform.branches(id, tenant_id, dealer_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_customer_authorizations_aftersales_case
        FOREIGN KEY (aftersales_case_id, tenant_id)
        REFERENCES platform.aftersales_cases(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_customer_authorizations_created_by
        FOREIGN KEY (created_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_customer_authorizations_updated_by
        FOREIGN KEY (updated_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_customer_authorizations_tenant_number
        UNIQUE (tenant_id, authorization_number),

    CONSTRAINT uq_customer_authorizations_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT ck_customer_authorizations_branch_requires_dealer
        CHECK (
            branch_id IS NULL
            OR dealer_id IS NOT NULL
        ),

    CONSTRAINT ck_customer_authorizations_scope_snapshot_object
        CHECK (
            jsonb_typeof(authorization_scope_snapshot) = 'object'
        ),

    CONSTRAINT ck_customer_authorizations_commercial_snapshot_object
        CHECK (
            commercial_snapshot IS NULL
            OR jsonb_typeof(commercial_snapshot) = 'object'
        ),

    CONSTRAINT ck_customer_authorizations_decided_at
        CHECK (
            decided_at IS NULL
            OR decided_at >= requested_at
        ),

    CONSTRAINT ck_customer_authorizations_decision_metadata
        CHECK (
            decided_at IS NOT NULL
            OR (
                decision_channel IS NULL
                AND decision_reference IS NULL
            )
        ),

    CONSTRAINT ck_customer_authorizations_version
        CHECK (version >= 0)
);

CREATE INDEX ix_customer_authorizations_tenant_id
    ON platform.customer_authorizations(tenant_id);

CREATE INDEX ix_customer_authorizations_dealer_id
    ON platform.customer_authorizations(dealer_id);

CREATE INDEX ix_customer_authorizations_branch_id
    ON platform.customer_authorizations(branch_id);

CREATE INDEX ix_customer_authorizations_aftersales_case_id
    ON platform.customer_authorizations(aftersales_case_id);

CREATE INDEX ix_customer_authorizations_tenant_status
    ON platform.customer_authorizations(
        tenant_id,
        authorization_status
    );

CREATE INDEX ix_customer_authorizations_tenant_case
    ON platform.customer_authorizations(
        tenant_id,
        aftersales_case_id
    );

CREATE INDEX ix_customer_authorizations_requested_at
    ON platform.customer_authorizations(requested_at);
