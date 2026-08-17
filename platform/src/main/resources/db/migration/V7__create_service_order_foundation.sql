-- Sprint 5 S5.3 Service Order persistence foundation.
--
-- Establishes ServiceOrder as an authoritative Core DMS transactional
-- aggregate for workshop work.
--
-- A ServiceOrder is independently creatable. Inspection, AI analysis,
-- Finding, CustomerAuthorization, AfterSalesCase, Quote, Appointment and
-- Invoice are deliberately not prerequisites and are not referenced by
-- this persistence foundation.
--
-- OrderLine is deliberately excluded from this migration and will be
-- introduced through the separately controlled S5.3.2 slice.
--
-- vehicle_id is mandatory because a ServiceOrder represents work for a
-- vehicle. The Java Core DMS platform does not yet own an authoritative
-- Vehicle persistence table, so this migration deliberately does not create
-- a speculative vehicle foreign key or shadow Vehicle aggregate. Vehicle
-- existence and tenant containment must be enforced when the authoritative
-- Java Vehicle ownership/bridge capability is introduced.
--
-- Financial valuation is deliberately excluded. This table must not imply
-- one ServiceOrder = one Invoice and must preserve future support for
-- partial/multiple invoices, credit notes, adjustments, reversals and
-- allocations.

CREATE TABLE platform.service_orders (
    id UUID PRIMARY KEY,

    tenant_id UUID NOT NULL,
    dealer_id UUID,
    branch_id UUID,

    order_number VARCHAR(80) NOT NULL,
    vehicle_id UUID NOT NULL,

    status VARCHAR(32) NOT NULL,

    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_service_orders_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_orders_dealer
        FOREIGN KEY (dealer_id, tenant_id)
        REFERENCES platform.dealers(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_orders_branch
        FOREIGN KEY (branch_id, tenant_id, dealer_id)
        REFERENCES platform.branches(id, tenant_id, dealer_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_orders_created_by
        FOREIGN KEY (created_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_orders_updated_by
        FOREIGN KEY (updated_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_orders_tenant_order_number
        UNIQUE (tenant_id, order_number),

    CONSTRAINT uq_service_orders_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT ck_service_orders_branch_requires_dealer
        CHECK (
            branch_id IS NULL
            OR dealer_id IS NOT NULL
        ),

    CONSTRAINT ck_service_orders_completed_at
        CHECK (
            completed_at IS NULL
            OR completed_at >= opened_at
        ),

    CONSTRAINT ck_service_orders_closed_at
        CHECK (
            closed_at IS NULL
            OR closed_at >= opened_at
        ),

    CONSTRAINT ck_service_orders_cancelled_at
        CHECK (
            cancelled_at IS NULL
            OR cancelled_at >= opened_at
        ),

    CONSTRAINT ck_service_orders_version
        CHECK (version >= 0)
);

CREATE INDEX ix_service_orders_tenant_id
    ON platform.service_orders(tenant_id);

CREATE INDEX ix_service_orders_dealer_id
    ON platform.service_orders(dealer_id);

CREATE INDEX ix_service_orders_branch_id
    ON platform.service_orders(branch_id);

CREATE INDEX ix_service_orders_vehicle_id
    ON platform.service_orders(vehicle_id);

CREATE INDEX ix_service_orders_tenant_status
    ON platform.service_orders(
        tenant_id,
        status
    );

CREATE INDEX ix_service_orders_tenant_vehicle
    ON platform.service_orders(
        tenant_id,
        vehicle_id
    );

CREATE INDEX ix_service_orders_opened_at
    ON platform.service_orders(opened_at);
