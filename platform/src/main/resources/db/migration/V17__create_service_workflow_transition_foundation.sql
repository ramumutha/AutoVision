-- Sprint 5 S5.4.3-B workflow transition configuration persistence foundation.
-- Transition definitions are configuration edges only; runtime movement is
-- intentionally deferred to a later slice.

CREATE TABLE platform.service_workflow_transitions (
    id UUID NOT NULL,
    workflow_version_id UUID NOT NULL,
    from_stage_id UUID NOT NULL,
    from_status_id UUID NOT NULL,
    to_stage_id UUID NOT NULL,
    to_status_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    sequence INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by_principal_id UUID NOT NULL,
    updated_by_principal_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_service_workflow_transitions
        PRIMARY KEY (id),

    CONSTRAINT fk_service_workflow_transitions_version
        FOREIGN KEY (workflow_version_id)
        REFERENCES platform.service_workflow_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_workflow_transitions_from_stage
        FOREIGN KEY (from_stage_id, workflow_version_id)
        REFERENCES platform.service_workflow_stages(id, workflow_version_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_workflow_transitions_from_status
        FOREIGN KEY (from_status_id, from_stage_id)
        REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_workflow_transitions_to_stage
        FOREIGN KEY (to_stage_id, workflow_version_id)
        REFERENCES platform.service_workflow_stages(id, workflow_version_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_workflow_transitions_to_status
        FOREIGN KEY (to_status_id, to_stage_id)
        REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_workflow_transitions_version_code
        UNIQUE (workflow_version_id, code),

    CONSTRAINT uq_service_workflow_transitions_version_edge
        UNIQUE (
            workflow_version_id,
            from_stage_id,
            from_status_id,
            to_stage_id,
            to_status_id
        ),

    CONSTRAINT ck_service_workflow_transitions_sequence
        CHECK (sequence >= 0),

    CONSTRAINT ck_service_workflow_transitions_version
        CHECK (version >= 0),

    CONSTRAINT ck_service_workflow_transitions_not_self_loop
        CHECK (NOT (
            from_stage_id = to_stage_id
            AND from_status_id = to_status_id
        ))
);

CREATE INDEX ix_service_workflow_transitions_version_id
    ON platform.service_workflow_transitions(workflow_version_id);

CREATE INDEX ix_service_workflow_transitions_from_state
    ON platform.service_workflow_transitions(
        workflow_version_id,
        from_stage_id,
        from_status_id
    );