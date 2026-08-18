-- Sprint 5 S5.4.1-B Service Workflow configuration persistence foundation.
-- Workflow configuration is independent from ServiceOrder lifecycle state.

CREATE TABLE platform.service_workflow_definitions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    dealer_id UUID,
    branch_id UUID,
    code VARCHAR(80) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by_principal_id UUID,
    updated_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_service_workflow_definitions
        PRIMARY KEY (id),

    CONSTRAINT fk_service_workflow_definitions_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_workflow_definitions_dealer
        FOREIGN KEY (dealer_id, tenant_id)
        REFERENCES platform.dealers(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_workflow_definitions_branch
        FOREIGN KEY (branch_id, tenant_id, dealer_id)
        REFERENCES platform.branches(id, tenant_id, dealer_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_workflow_definitions_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT ck_service_workflow_definitions_branch_requires_dealer
        CHECK (branch_id IS NULL OR dealer_id IS NOT NULL),

    CONSTRAINT ck_service_workflow_definitions_version
        CHECK (version >= 0)
);

CREATE INDEX ix_service_workflow_definitions_tenant_id
    ON platform.service_workflow_definitions(tenant_id);

CREATE INDEX ix_service_workflow_definitions_dealer_id
    ON platform.service_workflow_definitions(dealer_id);

CREATE INDEX ix_service_workflow_definitions_branch_id
    ON platform.service_workflow_definitions(branch_id);

CREATE UNIQUE INDEX uq_service_workflow_definitions_tenant_code
    ON platform.service_workflow_definitions(tenant_id, code)
    WHERE dealer_id IS NULL AND branch_id IS NULL;

CREATE UNIQUE INDEX uq_service_workflow_definitions_dealer_code
    ON platform.service_workflow_definitions(tenant_id, dealer_id, code)
    WHERE dealer_id IS NOT NULL AND branch_id IS NULL;

CREATE UNIQUE INDEX uq_service_workflow_definitions_branch_code
    ON platform.service_workflow_definitions(
        tenant_id,
        dealer_id,
        branch_id,
        code
    )
    WHERE dealer_id IS NOT NULL AND branch_id IS NOT NULL;


CREATE TABLE platform.service_workflow_versions (
    id UUID NOT NULL,
    workflow_definition_id UUID NOT NULL,
    version_number BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_by_principal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_by_principal_id UUID,
    published_at TIMESTAMPTZ,

    CONSTRAINT pk_service_workflow_versions
        PRIMARY KEY (id),

    CONSTRAINT fk_service_workflow_versions_definition
        FOREIGN KEY (workflow_definition_id)
        REFERENCES platform.service_workflow_definitions(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_workflow_versions_definition_number
        UNIQUE (workflow_definition_id, version_number),

    CONSTRAINT ck_service_workflow_versions_number
        CHECK (version_number > 0),

    CONSTRAINT ck_service_workflow_versions_lock_version
        CHECK (lock_version >= 0),

    CONSTRAINT ck_service_workflow_versions_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),

    CONSTRAINT ck_service_workflow_versions_publication_metadata
        CHECK (
            (
                status = 'PUBLISHED'
                AND published_by_principal_id IS NOT NULL
                AND published_at IS NOT NULL
            )
            OR
            (
                status <> 'PUBLISHED'
            )
        ),

    CONSTRAINT ck_service_workflow_versions_draft_metadata
        CHECK (
            status <> 'DRAFT'
            OR (
                published_by_principal_id IS NULL
                AND published_at IS NULL
            )
        )
);

CREATE INDEX ix_service_workflow_versions_definition_id
    ON platform.service_workflow_versions(workflow_definition_id);


CREATE TABLE platform.service_workflow_stages (
    id UUID NOT NULL,
    workflow_version_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    sequence INTEGER NOT NULL,
    active BOOLEAN NOT NULL,

    CONSTRAINT pk_service_workflow_stages
        PRIMARY KEY (id),

    CONSTRAINT fk_service_workflow_stages_version
        FOREIGN KEY (workflow_version_id)
        REFERENCES platform.service_workflow_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_workflow_stages_version_code
        UNIQUE (workflow_version_id, code),

    CONSTRAINT ck_service_workflow_stages_sequence
        CHECK (sequence >= 0)
);

CREATE INDEX ix_service_workflow_stages_version_id
    ON platform.service_workflow_stages(workflow_version_id);


CREATE TABLE platform.service_workflow_statuses (
    id UUID NOT NULL,
    workflow_stage_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    sequence INTEGER NOT NULL,
    active BOOLEAN NOT NULL,

    CONSTRAINT pk_service_workflow_statuses
        PRIMARY KEY (id),

    CONSTRAINT fk_service_workflow_statuses_stage
        FOREIGN KEY (workflow_stage_id)
        REFERENCES platform.service_workflow_stages(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_workflow_statuses_stage_code
        UNIQUE (workflow_stage_id, code),

    CONSTRAINT ck_service_workflow_statuses_sequence
        CHECK (sequence >= 0)
);

CREATE INDEX ix_service_workflow_statuses_stage_id
    ON platform.service_workflow_statuses(workflow_stage_id);
