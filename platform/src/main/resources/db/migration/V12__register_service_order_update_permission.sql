-- Sprint 5 ServiceOrder mutation authorization capability.
--
-- Registers the platform-defined permission required for resource-aware
-- ServiceOrder lifecycle mutations.
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
    '3b9ad7d8-d164-4b46-a584-883a6796e120',
    'SERVICE_ORDER.UPDATE',
    'SERVICE_ORDER',
    'UPDATE',
    'Update the lifecycle of a ServiceOrder within the authorized organizational scope.',
    TRUE,
    TRUE
);