-- Dealer-actionable display context retained from authoritative ingestion.
-- This read model is not canonical customer, vehicle or service master data.

CREATE TABLE platform.service_profit_opportunity_contexts (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    opportunity_id UUID NOT NULL,

    customer_display_name VARCHAR(200),
    customer_reference VARCHAR(120),
    customer_phone VARCHAR(80),
    customer_email VARCHAR(320),
    customer_contactable BOOLEAN,

    vehicle_registration VARCHAR(80),
    vehicle_vin VARCHAR(80),
    vehicle_make VARCHAR(120),
    vehicle_model VARCHAR(120),
    vehicle_model_year INTEGER,
    vehicle_powertrain VARCHAR(80),

    service_order_reference VARCHAR(120),
    service_date DATE,
    service_description VARCHAR(1000),
    service_advisor_context VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_service_profit_opportunity_contexts
        PRIMARY KEY (id),

    CONSTRAINT uq_service_profit_opportunity_contexts_opportunity
        UNIQUE (opportunity_id),

    CONSTRAINT uq_service_profit_opportunity_contexts_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT fk_service_profit_opportunity_contexts_opportunity
        FOREIGN KEY (opportunity_id, tenant_id)
        REFERENCES platform.service_profit_opportunities(id, tenant_id)
        ON DELETE CASCADE,

    CONSTRAINT ck_service_profit_opportunity_contexts_model_year
        CHECK (
            vehicle_model_year IS NULL
            OR vehicle_model_year BETWEEN 1886 AND 2200
        )
);

CREATE INDEX ix_service_profit_opportunity_contexts_tenant_opportunity
    ON platform.service_profit_opportunity_contexts(
        tenant_id,
        opportunity_id
    );