CREATE TABLE platform.locations (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(120),
    state_province VARCHAR(120),
    postal_code VARCHAR(40),
    country_code VARCHAR(2),
    timezone VARCHAR(80),
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_locations PRIMARY KEY (id),
    CONSTRAINT fk_locations_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_locations_tenant_code
        UNIQUE (tenant_id, code),

    CONSTRAINT uq_locations_tenant_id
        UNIQUE (tenant_id, id)
);

CREATE INDEX ix_locations_tenant_id
    ON platform.locations (tenant_id);


CREATE TABLE platform.dealers (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    primary_location_id UUID,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_dealers PRIMARY KEY (id),
    CONSTRAINT fk_dealers_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_dealers_tenant_code
        UNIQUE (tenant_id, code),

    CONSTRAINT uq_dealers_tenant_id
        UNIQUE (tenant_id, id),

    CONSTRAINT fk_dealers_primary_location
        FOREIGN KEY (tenant_id, primary_location_id)
        REFERENCES platform.locations(tenant_id, id)
);

CREATE INDEX ix_dealers_tenant_id
    ON platform.dealers (tenant_id);

CREATE INDEX ix_dealers_primary_location_id
    ON platform.dealers (primary_location_id);


CREATE TABLE platform.branches (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    dealer_id UUID NOT NULL,
    location_id UUID,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_branches PRIMARY KEY (id),

    CONSTRAINT fk_branches_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_branches_dealer
        FOREIGN KEY (tenant_id, dealer_id)
        REFERENCES platform.dealers(tenant_id, id)
        ON DELETE CASCADE,

    CONSTRAINT fk_branches_location
        FOREIGN KEY (tenant_id, location_id)
        REFERENCES platform.locations(tenant_id, id),

    CONSTRAINT uq_branches_tenant_dealer_code
        UNIQUE (tenant_id, dealer_id, code),

    CONSTRAINT uq_branches_tenant_id
        UNIQUE (tenant_id, id)
);

CREATE INDEX ix_branches_tenant_id
    ON platform.branches (tenant_id);

CREATE INDEX ix_branches_dealer_id
    ON platform.branches (dealer_id);

CREATE INDEX ix_branches_location_id
    ON platform.branches (location_id);