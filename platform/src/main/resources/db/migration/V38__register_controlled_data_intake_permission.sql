-- Registers the least-privilege permission for one authorized controlled
-- dealer-data package to enter the existing intake pipeline.

INSERT INTO platform.permissions (
    id, code, resource, action, description, system_defined, is_active
)
VALUES (
    '1cf2f21a-32db-4eb8-9d71-7a1c2c0a4a01',
    'SERVICE_PROFIT_CONTROLLED_DATA_INTAKE.PROCESS',
    'SERVICE_PROFIT_CONTROLLED_DATA_INTAKE',
    'PROCESS',
    'Process an authorized controlled dealer-data package within the permitted organizational scope.',
    TRUE,
    TRUE
);