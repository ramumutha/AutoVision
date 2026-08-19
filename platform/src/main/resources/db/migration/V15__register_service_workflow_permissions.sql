-- Sprint 5 Service Workflow configuration authorization permission catalog.
--
-- Registers platform-defined permission capabilities for workflow
-- configuration. This migration deliberately does not assign permissions to
-- roles, permission sets, principals, tenants, dealers or branches.

INSERT INTO platform.permissions (
    id,
    code,
    resource,
    action,
    description,
    system_defined,
    is_active
)
VALUES
    (
        '84ef58d1-cab0-4c69-9c93-764a7800e108',
        'SERVICE_WORKFLOW.READ',
        'SERVICE_WORKFLOW',
        'READ',
        'Read Service Workflow configuration within the authorized organizational scope.',
        TRUE,
        TRUE
    ),
    (
        '84ef58d1-cab0-4c69-9c93-764a7800e109',
        'SERVICE_WORKFLOW.MANAGE',
        'SERVICE_WORKFLOW',
        'MANAGE',
        'Create and manage draft Service Workflow configuration within the authorized organizational scope.',
        TRUE,
        TRUE
    ),
    (
        '84ef58d1-cab0-4c69-9c93-764a7800e10a',
        'SERVICE_WORKFLOW.PUBLISH',
        'SERVICE_WORKFLOW',
        'PUBLISH',
        'Publish and retire Service Workflow versions within the authorized organizational scope.',
        TRUE,
        TRUE
    );