-- Sprint 5 S5.4.2-B runtime workflow execution persistence foundation.
-- Runtime execution pins configuration identities without introducing
-- ServiceOrder workflow behavior or configuration relationships.

ALTER TABLE platform.service_workflow_versions
    ADD CONSTRAINT uq_service_workflow_versions_id_definition
    UNIQUE (id, workflow_definition_id);

ALTER TABLE platform.service_workflow_stages
    ADD CONSTRAINT uq_service_workflow_stages_id_version
    UNIQUE (id, workflow_version_id);

ALTER TABLE platform.service_workflow_statuses
    ADD CONSTRAINT uq_service_workflow_statuses_id_stage
    UNIQUE (id, workflow_stage_id);

CREATE TABLE platform.service_order_workflow_executions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    service_order_id UUID NOT NULL,
    workflow_definition_id UUID NOT NULL,
    workflow_version_id UUID NOT NULL,
    current_stage_id UUID NOT NULL,
    current_status_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by_principal_id UUID NOT NULL,
    updated_by_principal_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_service_order_workflow_executions
        PRIMARY KEY (id),

    CONSTRAINT uq_service_order_workflow_executions_id_tenant
        UNIQUE (id, tenant_id),

    CONSTRAINT uq_service_order_workflow_executions_service_order
        UNIQUE (service_order_id),

    CONSTRAINT fk_service_order_workflow_executions_order
        FOREIGN KEY (service_order_id, tenant_id)
        REFERENCES platform.service_orders(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_executions_definition
        FOREIGN KEY (workflow_definition_id, tenant_id)
        REFERENCES platform.service_workflow_definitions(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_executions_version
        FOREIGN KEY (workflow_version_id, workflow_definition_id)
        REFERENCES platform.service_workflow_versions(id, workflow_definition_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_executions_stage
        FOREIGN KEY (current_stage_id, workflow_version_id)
        REFERENCES platform.service_workflow_stages(id, workflow_version_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_executions_status
        FOREIGN KEY (current_status_id, current_stage_id)
        REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_service_order_workflow_executions_version
        CHECK (version >= 0)
);

CREATE INDEX ix_service_order_workflow_executions_tenant_id
    ON platform.service_order_workflow_executions(tenant_id);

CREATE INDEX ix_service_order_workflow_executions_workflow_definition_id
    ON platform.service_order_workflow_executions(workflow_definition_id);

CREATE INDEX ix_service_order_workflow_executions_workflow_version_id
    ON platform.service_order_workflow_executions(workflow_version_id);