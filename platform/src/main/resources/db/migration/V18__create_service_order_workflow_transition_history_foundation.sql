-- Sprint 5 S5.4.4-D runtime transition history/audit foundation.
-- Append-only record of completed ServiceOrderWorkflowExecution movements.
-- This migration does not integrate history creation into the runtime
-- transition command; it only establishes a trustworthy persistence target.

ALTER TABLE platform.service_order_workflow_executions
    ADD CONSTRAINT uq_service_order_workflow_executions_id_tenant_order
    UNIQUE (id, tenant_id, service_order_id);

ALTER TABLE platform.service_workflow_transitions
    ADD CONSTRAINT uq_service_workflow_transitions_id_version
    UNIQUE (id, workflow_version_id);

CREATE TABLE platform.service_order_workflow_transition_histories (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    service_order_id UUID NOT NULL,
    workflow_execution_id UUID NOT NULL,
    workflow_definition_id UUID NOT NULL,
    workflow_version_id UUID NOT NULL,
    transition_id UUID NOT NULL,
    from_stage_id UUID NOT NULL,
    from_status_id UUID NOT NULL,
    to_stage_id UUID NOT NULL,
    to_status_id UUID NOT NULL,
    executed_by_principal_id UUID NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_service_order_workflow_transition_histories
        PRIMARY KEY (id),

    CONSTRAINT fk_service_order_workflow_transition_histories_order
        FOREIGN KEY (service_order_id, tenant_id)
        REFERENCES platform.service_orders(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_transition_histories_execution
        FOREIGN KEY (workflow_execution_id, tenant_id, service_order_id)
        REFERENCES platform.service_order_workflow_executions(
            id, tenant_id, service_order_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_transition_histories_definition
        FOREIGN KEY (workflow_definition_id, tenant_id)
        REFERENCES platform.service_workflow_definitions(id, tenant_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_transition_histories_version
        FOREIGN KEY (workflow_version_id, workflow_definition_id)
        REFERENCES platform.service_workflow_versions(id, workflow_definition_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_transition_histories_transition
        FOREIGN KEY (transition_id, workflow_version_id)
        REFERENCES platform.service_workflow_transitions(id, workflow_version_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_transition_histories_from_stage
        FOREIGN KEY (from_stage_id, workflow_version_id)
        REFERENCES platform.service_workflow_stages(id, workflow_version_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_transition_histories_from_status
        FOREIGN KEY (from_status_id, from_stage_id)
        REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_transition_histories_to_stage
        FOREIGN KEY (to_stage_id, workflow_version_id)
        REFERENCES platform.service_workflow_stages(id, workflow_version_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_order_workflow_transition_histories_to_status
        FOREIGN KEY (to_status_id, to_stage_id)
        REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_service_order_workflow_transition_histories_not_self_loop
        CHECK (NOT (
            from_stage_id = to_stage_id
            AND from_status_id = to_status_id
        ))
);

CREATE INDEX ix_service_order_workflow_transition_histories_order_time
    ON platform.service_order_workflow_transition_histories(
        service_order_id, executed_at);

CREATE INDEX ix_service_order_workflow_transition_histories_execution_time
    ON platform.service_order_workflow_transition_histories(
        workflow_execution_id, executed_at);

CREATE INDEX ix_service_order_workflow_transition_histories_tenant_time
    ON platform.service_order_workflow_transition_histories(
        tenant_id, executed_at);
