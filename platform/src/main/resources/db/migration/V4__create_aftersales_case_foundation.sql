-- Sprint 5 AfterSales Case persistence foundation.
-- Establishes the durable Core DMS aftersales business-case context.
-- Inspection and AI capabilities remain optional add-ons and are not
-- prerequisites for creation or lifecycle progression of an AfterSalesCase.

-- Provide a tenant-safe dealer-aware candidate key so AfterSalesCase can
-- enforce that a selected branch belongs to its selected dealer.
ALTER TABLE platform.branches
    ADD CONSTRAINT uq_branches_id_tenant_dealer
        UNIQUE (id, tenant_id, dealer_id);


CREATE TABLE platform.aftersales_cases (
    id UUID PRIMARY KEY,

    tenant_id UUID NOT NULL,
    dealer_id UUID,
    branch_id UUID,

    case_number VARCHAR(80) NOT NULL,
    lifecycle_status VARCHAR(32) NOT NULL,
    source_channel VARCHAR(32),

    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_aftersales_cases_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_aftersales_cases_dealer
        FOREIGN KEY (dealer_id, tenant_id)
        REFERENCES platform.dealers(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_aftersales_cases_branch
        FOREIGN KEY (branch_id, tenant_id, dealer_id)
        REFERENCES platform.branches(id, tenant_id, dealer_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_aftersales_cases_created_by
        FOREIGN KEY (created_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_aftersales_cases_updated_by
        FOREIGN KEY (updated_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_aftersales_cases_tenant_case_number
        UNIQUE (tenant_id, case_number),

    CONSTRAINT uq_aftersales_cases_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT ck_aftersales_cases_branch_requires_dealer
        CHECK (
            branch_id IS NULL
            OR dealer_id IS NOT NULL
        ),

    CONSTRAINT ck_aftersales_cases_closed_at
        CHECK (
            closed_at IS NULL
            OR closed_at >= opened_at
        ),

    CONSTRAINT ck_aftersales_cases_version
        CHECK (version >= 0)
);

CREATE INDEX ix_aftersales_cases_tenant_id
    ON platform.aftersales_cases(tenant_id);

CREATE INDEX ix_aftersales_cases_dealer_id
    ON platform.aftersales_cases(dealer_id);

CREATE INDEX ix_aftersales_cases_branch_id
    ON platform.aftersales_cases(branch_id);

CREATE INDEX ix_aftersales_cases_tenant_lifecycle_status
    ON platform.aftersales_cases(tenant_id, lifecycle_status);

CREATE INDEX ix_aftersales_cases_opened_at
    ON platform.aftersales_cases(opened_at);
