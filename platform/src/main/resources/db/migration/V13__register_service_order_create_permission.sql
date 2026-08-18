-- Sprint 5 ServiceOrder creation authorization capability.
--
-- Registers the platform-defined permission required for resource-aware
-- ServiceOrder creation.
--
-- This migration deliberately does not assign the permission to roles,
-- permission sets, principals, tenants, dealers, branches, or groups.
-- Authorization assignments remain configurable through the existing
-- least-privilege authorization model.

INSERT INTO platform.permissions (
    id,
    code,
    resource,
    action,
    description,
    system_defined,
    is_active
)
VALUES (
    'f2a6d4b1-1cb4-4e8a-9d7e-52a8f0c31b64',
    'SERVICE_ORDER.CREATE',
    'SERVICE_ORDER',
    'CREATE',
    'Create a ServiceOrder within the authorized organizational scope.',
    TRUE,
    TRUE
);