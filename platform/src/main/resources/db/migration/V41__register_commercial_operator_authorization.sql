-- Establishes the least-privilege system-scoped capability for internal
-- commercial verification operations. Assignment to a human principal is
-- intentionally performed by the local operator bootstrap, not by migration.

INSERT INTO platform.permissions (
    id, code, resource, action, description, system_defined, is_active
)
VALUES (
    '4a2f94ad-56b6-4f97-92cc-0fcf4a4f2c01',
    'COMMERCIAL_ENQUIRY.OPERATE',
    'COMMERCIAL_ENQUIRY',
    'OPERATE',
    'Operate internal commercial enquiry verification workflows.',
    TRUE,
    TRUE
);

INSERT INTO platform.roles (
    id, code, name, description, system_defined, is_protected, is_active
)
VALUES (
    '4a2f94ad-56b6-4f97-92cc-0fcf4a4f2c02',
    'VERSPEN_COMMERCIAL_OPERATOR',
    'VERSPEN Commercial Operator',
    'Internal system-scoped role for commercial enquiry operations.',
    TRUE,
    TRUE,
    TRUE
);

INSERT INTO platform.role_permissions (role_id, permission_id)
VALUES (
    '4a2f94ad-56b6-4f97-92cc-0fcf4a4f2c02',
    '4a2f94ad-56b6-4f97-92cc-0fcf4a4f2c01'
);