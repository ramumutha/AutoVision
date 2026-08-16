-- Sprint 4 authorization foundation.
-- Creates organizational grouping and configurable authorization structures.
-- Existing public.role_assignments remains untouched for compatibility.

CREATE TABLE platform.tenant_groups (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_tenant_groups_code
        UNIQUE (code),

    CONSTRAINT ck_tenant_groups_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE platform.tenant_group_memberships (
    id UUID PRIMARY KEY,
    tenant_group_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    membership_type VARCHAR(32) NOT NULL DEFAULT 'OWNERSHIP',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_tenant_group_memberships_group
        FOREIGN KEY (tenant_group_id)
        REFERENCES platform.tenant_groups(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tenant_group_memberships_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_tenant_group_memberships_group_tenant
        UNIQUE (tenant_group_id, tenant_id),

    CONSTRAINT ck_tenant_group_memberships_type
        CHECK (membership_type IN ('OWNERSHIP'))
);

CREATE INDEX ix_tenant_group_memberships_tenant_group_id
    ON platform.tenant_group_memberships(tenant_group_id);

CREATE INDEX ix_tenant_group_memberships_tenant_id
    ON platform.tenant_group_memberships(tenant_id);


CREATE TABLE platform.dealer_groups (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_dealer_groups_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_dealer_groups_tenant_code
        UNIQUE (tenant_id, code),

    CONSTRAINT uq_dealer_groups_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT ck_dealer_groups_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX ix_dealer_groups_tenant_id
    ON platform.dealer_groups(tenant_id);


ALTER TABLE platform.dealers
    ADD CONSTRAINT uq_dealers_id_tenant
        UNIQUE (id, tenant_id);


CREATE TABLE platform.dealer_group_memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    dealer_group_id UUID NOT NULL,
    dealer_id UUID NOT NULL,
    membership_type VARCHAR(32) NOT NULL DEFAULT 'OWNERSHIP',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_dealer_group_memberships_group
        FOREIGN KEY (dealer_group_id, tenant_id)
        REFERENCES platform.dealer_groups(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_dealer_group_memberships_dealer
        FOREIGN KEY (dealer_id, tenant_id)
        REFERENCES platform.dealers(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_dealer_group_memberships_group_dealer
        UNIQUE (dealer_group_id, dealer_id),

    CONSTRAINT ck_dealer_group_memberships_type
        CHECK (membership_type IN ('OWNERSHIP'))
);

CREATE INDEX ix_dealer_group_memberships_dealer_group_id
    ON platform.dealer_group_memberships(dealer_group_id);

CREATE INDEX ix_dealer_group_memberships_dealer_id
    ON platform.dealer_group_memberships(dealer_id);

CREATE INDEX ix_dealer_group_memberships_tenant_id
    ON platform.dealer_group_memberships(tenant_id);


CREATE TABLE platform.authorization_principals (
    id UUID PRIMARY KEY,
    principal_type VARCHAR(32) NOT NULL,
    user_ref_id UUID,
    tenant_id UUID,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_authorization_principals_user_ref
        FOREIGN KEY (user_ref_id)
        REFERENCES public.user_refs(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_authorization_principals_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_authorization_principals_issuer_subject
        UNIQUE (issuer, subject),

    CONSTRAINT ck_authorization_principals_type
        CHECK (
            principal_type IN (
                'HUMAN',
                'SERVICE',
                'INTEGRATION',
                'SYSTEM'
            )
        ),

    CONSTRAINT ck_authorization_principals_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT ck_authorization_principals_system_scope
        CHECK (
            (
                principal_type = 'SYSTEM'
                AND user_ref_id IS NULL
                AND tenant_id IS NULL
            )
            OR
            (
                principal_type <> 'SYSTEM'
            )
        )
);

CREATE INDEX ix_authorization_principals_user_ref_id
    ON platform.authorization_principals(user_ref_id);

CREATE INDEX ix_authorization_principals_tenant_id
    ON platform.authorization_principals(tenant_id);


CREATE TABLE platform.permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(160) NOT NULL,
    resource VARCHAR(80) NOT NULL,
    action VARCHAR(80) NOT NULL,
    description VARCHAR(1000),
    system_defined BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_permissions_code
        UNIQUE (code),

    CONSTRAINT uq_permissions_resource_action
        UNIQUE (resource, action)
);


CREATE TABLE platform.permission_sets (
    id UUID PRIMARY KEY,
    tenant_group_id UUID,
    tenant_id UUID,
    code VARCHAR(160) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    system_defined BOOLEAN NOT NULL DEFAULT FALSE,
    is_protected BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_permission_sets_tenant_group
        FOREIGN KEY (tenant_group_id)
        REFERENCES platform.tenant_groups(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_permission_sets_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_permission_sets_single_owner
        CHECK (
            NOT (
                tenant_group_id IS NOT NULL
                AND tenant_id IS NOT NULL
            )
        )
);

CREATE UNIQUE INDEX uq_permission_sets_system_code
    ON platform.permission_sets(code)
    WHERE tenant_group_id IS NULL
      AND tenant_id IS NULL;

CREATE UNIQUE INDEX uq_permission_sets_tenant_group_code
    ON platform.permission_sets(tenant_group_id, code)
    WHERE tenant_group_id IS NOT NULL
      AND tenant_id IS NULL;

CREATE UNIQUE INDEX uq_permission_sets_tenant_code
    ON platform.permission_sets(tenant_id, code)
    WHERE tenant_id IS NOT NULL
      AND tenant_group_id IS NULL;

CREATE INDEX ix_permission_sets_tenant_group_id
    ON platform.permission_sets(tenant_group_id);

CREATE INDEX ix_permission_sets_tenant_id
    ON platform.permission_sets(tenant_id);


CREATE TABLE platform.permission_set_permissions (
    permission_set_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_permission_set_permissions
        PRIMARY KEY (permission_set_id, permission_id),

    CONSTRAINT fk_permission_set_permissions_set
        FOREIGN KEY (permission_set_id)
        REFERENCES platform.permission_sets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_permission_set_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES platform.permissions(id)
        ON DELETE RESTRICT
);


CREATE TABLE platform.roles (
    id UUID PRIMARY KEY,
    tenant_group_id UUID,
    tenant_id UUID,
    code VARCHAR(160) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    system_defined BOOLEAN NOT NULL DEFAULT FALSE,
    is_protected BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_roles_tenant_group
        FOREIGN KEY (tenant_group_id)
        REFERENCES platform.tenant_groups(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_roles_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_roles_single_owner
        CHECK (
            NOT (
                tenant_group_id IS NOT NULL
                AND tenant_id IS NOT NULL
            )
        )
);

CREATE UNIQUE INDEX uq_roles_system_code
    ON platform.roles(code)
    WHERE tenant_group_id IS NULL
      AND tenant_id IS NULL;

CREATE UNIQUE INDEX uq_roles_tenant_group_code
    ON platform.roles(tenant_group_id, code)
    WHERE tenant_group_id IS NOT NULL
      AND tenant_id IS NULL;

CREATE UNIQUE INDEX uq_roles_tenant_code
    ON platform.roles(tenant_id, code)
    WHERE tenant_id IS NOT NULL
      AND tenant_group_id IS NULL;

CREATE INDEX ix_roles_tenant_group_id
    ON platform.roles(tenant_group_id);

CREATE INDEX ix_roles_tenant_id
    ON platform.roles(tenant_id);


CREATE TABLE platform.role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_role_permissions
        PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES platform.roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES platform.permissions(id)
        ON DELETE RESTRICT
);


CREATE TABLE platform.role_permission_sets (
    role_id UUID NOT NULL,
    permission_set_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_role_permission_sets
        PRIMARY KEY (role_id, permission_set_id),

    CONSTRAINT fk_role_permission_sets_role
        FOREIGN KEY (role_id)
        REFERENCES platform.roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permission_sets_permission_set
        FOREIGN KEY (permission_set_id)
        REFERENCES platform.permission_sets(id)
        ON DELETE RESTRICT
);


ALTER TABLE platform.branches
    ADD CONSTRAINT uq_branches_id_tenant
        UNIQUE (id, tenant_id);

ALTER TABLE platform.locations
    ADD CONSTRAINT uq_locations_id_tenant
        UNIQUE (id, tenant_id);


CREATE TABLE platform.scoped_role_assignments (
    id UUID PRIMARY KEY,

    principal_id UUID NOT NULL,
    role_id UUID NOT NULL,

    scope_type VARCHAR(32) NOT NULL,

    tenant_group_id UUID,
    tenant_id UUID,
    dealer_group_id UUID,
    dealer_id UUID,
    branch_id UUID,
    location_id UUID,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,

    created_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_scoped_role_assignments_principal
        FOREIGN KEY (principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_scoped_role_assignments_role
        FOREIGN KEY (role_id)
        REFERENCES platform.roles(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_scoped_role_assignments_tenant_group
        FOREIGN KEY (tenant_group_id)
        REFERENCES platform.tenant_groups(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_scoped_role_assignments_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_scoped_role_assignments_dealer_group
        FOREIGN KEY (dealer_group_id)
        REFERENCES platform.dealer_groups(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_scoped_role_assignments_dealer
        FOREIGN KEY (dealer_id)
        REFERENCES platform.dealers(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_scoped_role_assignments_branch
        FOREIGN KEY (branch_id)
        REFERENCES platform.branches(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_scoped_role_assignments_location
        FOREIGN KEY (location_id)
        REFERENCES platform.locations(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_scoped_role_assignments_created_by
        FOREIGN KEY (created_by_principal_id)
        REFERENCES platform.authorization_principals(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_scoped_role_assignments_scope_type
        CHECK (
            scope_type IN (
                'SYSTEM',
                'TENANT_GROUP',
                'TENANT',
                'DEALER_GROUP',
                'DEALER',
                'BRANCH',
                'LOCATION'
            )
        ),

    CONSTRAINT ck_scoped_role_assignments_scope_target
        CHECK (
            (
                scope_type = 'SYSTEM'
                AND tenant_group_id IS NULL
                AND tenant_id IS NULL
                AND dealer_group_id IS NULL
                AND dealer_id IS NULL
                AND branch_id IS NULL
                AND location_id IS NULL
            )
            OR
            (
                scope_type = 'TENANT_GROUP'
                AND tenant_group_id IS NOT NULL
                AND tenant_id IS NULL
                AND dealer_group_id IS NULL
                AND dealer_id IS NULL
                AND branch_id IS NULL
                AND location_id IS NULL
            )
            OR
            (
                scope_type = 'TENANT'
                AND tenant_group_id IS NULL
                AND tenant_id IS NOT NULL
                AND dealer_group_id IS NULL
                AND dealer_id IS NULL
                AND branch_id IS NULL
                AND location_id IS NULL
            )
            OR
            (
                scope_type = 'DEALER_GROUP'
                AND tenant_group_id IS NULL
                AND tenant_id IS NULL
                AND dealer_group_id IS NOT NULL
                AND dealer_id IS NULL
                AND branch_id IS NULL
                AND location_id IS NULL
            )
            OR
            (
                scope_type = 'DEALER'
                AND tenant_group_id IS NULL
                AND tenant_id IS NULL
                AND dealer_group_id IS NULL
                AND dealer_id IS NOT NULL
                AND branch_id IS NULL
                AND location_id IS NULL
            )
            OR
            (
                scope_type = 'BRANCH'
                AND tenant_group_id IS NULL
                AND tenant_id IS NULL
                AND dealer_group_id IS NULL
                AND dealer_id IS NULL
                AND branch_id IS NOT NULL
                AND location_id IS NULL
            )
            OR
            (
                scope_type = 'LOCATION'
                AND tenant_group_id IS NULL
                AND tenant_id IS NULL
                AND dealer_group_id IS NULL
                AND dealer_id IS NULL
                AND branch_id IS NULL
                AND location_id IS NOT NULL
            )
        ),

    CONSTRAINT ck_scoped_role_assignments_validity
        CHECK (
            valid_until IS NULL
            OR valid_from IS NULL
            OR valid_until > valid_from
        )
);


CREATE UNIQUE INDEX uq_scoped_role_assignments_system
    ON platform.scoped_role_assignments(principal_id, role_id)
    WHERE scope_type = 'SYSTEM';

CREATE UNIQUE INDEX uq_scoped_role_assignments_tenant_group
    ON platform.scoped_role_assignments(
        principal_id,
        role_id,
        tenant_group_id
    )
    WHERE scope_type = 'TENANT_GROUP';

CREATE UNIQUE INDEX uq_scoped_role_assignments_tenant
    ON platform.scoped_role_assignments(
        principal_id,
        role_id,
        tenant_id
    )
    WHERE scope_type = 'TENANT';

CREATE UNIQUE INDEX uq_scoped_role_assignments_dealer_group
    ON platform.scoped_role_assignments(
        principal_id,
        role_id,
        dealer_group_id
    )
    WHERE scope_type = 'DEALER_GROUP';

CREATE UNIQUE INDEX uq_scoped_role_assignments_dealer
    ON platform.scoped_role_assignments(
        principal_id,
        role_id,
        dealer_id
    )
    WHERE scope_type = 'DEALER';

CREATE UNIQUE INDEX uq_scoped_role_assignments_branch
    ON platform.scoped_role_assignments(
        principal_id,
        role_id,
        branch_id
    )
    WHERE scope_type = 'BRANCH';

CREATE UNIQUE INDEX uq_scoped_role_assignments_location
    ON platform.scoped_role_assignments(
        principal_id,
        role_id,
        location_id
    )
    WHERE scope_type = 'LOCATION';


CREATE INDEX ix_scoped_role_assignments_principal_id
    ON platform.scoped_role_assignments(principal_id);

CREATE INDEX ix_scoped_role_assignments_role_id
    ON platform.scoped_role_assignments(role_id);

CREATE INDEX ix_scoped_role_assignments_scope_type
    ON platform.scoped_role_assignments(scope_type);

CREATE INDEX ix_scoped_role_assignments_tenant_group_id
    ON platform.scoped_role_assignments(tenant_group_id);

CREATE INDEX ix_scoped_role_assignments_tenant_id
    ON platform.scoped_role_assignments(tenant_id);

CREATE INDEX ix_scoped_role_assignments_dealer_group_id
    ON platform.scoped_role_assignments(dealer_group_id);

CREATE INDEX ix_scoped_role_assignments_dealer_id
    ON platform.scoped_role_assignments(dealer_id);

CREATE INDEX ix_scoped_role_assignments_branch_id
    ON platform.scoped_role_assignments(branch_id);

CREATE INDEX ix_scoped_role_assignments_location_id
    ON platform.scoped_role_assignments(location_id);