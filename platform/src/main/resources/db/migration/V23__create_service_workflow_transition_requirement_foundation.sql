-- Sprint 5 S5.8.7-H transition-level process requirement binding foundation.
-- Bindings are configuration only; runtime enforcement is intentionally deferred.

CREATE TABLE platform.service_workflow_transition_requirements (
    id UUID NOT NULL,
    workflow_version_id UUID NOT NULL,
    workflow_transition_id UUID NOT NULL,
    requirement_key VARCHAR(120) NOT NULL,
    requirement_mode VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by_principal_id UUID,

    CONSTRAINT pk_service_workflow_transition_requirements
        PRIMARY KEY (id),

    CONSTRAINT fk_service_workflow_transition_requirements_version
        FOREIGN KEY (workflow_version_id)
        REFERENCES platform.service_workflow_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_workflow_transition_requirements_transition
        FOREIGN KEY (workflow_transition_id)
        REFERENCES platform.service_workflow_transitions(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_service_workflow_transition_requirements_transition_key
        UNIQUE (workflow_transition_id, requirement_key),

    CONSTRAINT ck_service_workflow_transition_requirements_key_not_blank
        CHECK (btrim(requirement_key) <> ''),

    CONSTRAINT ck_service_workflow_transition_requirements_mode
        CHECK (requirement_mode IN (
            'REQUIRED',
            'OPTIONAL',
            'CONDITIONAL',
            'AUTOMATIC',
            'NOT_APPLICABLE'
        ))
);

CREATE INDEX ix_service_workflow_transition_requirements_transition
    ON platform.service_workflow_transition_requirements(
        workflow_version_id,
        workflow_transition_id,
        created_at,
        id
    );