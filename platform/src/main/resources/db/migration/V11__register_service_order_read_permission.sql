-- Sprint 5 ServiceOrder read authorization capability.
--
-- Registers the platform-defined permission required for resource-aware
-- ServiceOrder reads.
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
    '84ef58d1-cab0-4c69-9c93-764a7800e107',
    'SERVICE_ORDER.READ',
    'SERVICE_ORDER',
    'READ',
    'Read a ServiceOrder and its contained aggregate within the authorized organizational scope.',
    TRUE,
    TRUE
);