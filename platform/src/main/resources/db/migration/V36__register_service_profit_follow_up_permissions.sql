-- AutoVision Service Profit follow-up authorization capability.
--
-- Registers bounded read and operational-management permissions only.
-- Role and tenant assignments remain configurable through the existing
-- authorization administration model.

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
    '7bdb8c90-5b83-4a7b-86ae-3d4e5e3d8c01',
    'SERVICE_PROFIT_FOLLOW_UP.READ',
    'SERVICE_PROFIT_FOLLOW_UP',
    'READ',
    'Read the current Service Profit follow-up and its ordered internal history.',
    TRUE,
    TRUE
),
(
    '7bdb8c90-5b83-4a7b-86ae-3d4e5e3d8c02',
    'SERVICE_PROFIT_FOLLOW_UP.MANAGE',
    'SERVICE_PROFIT_FOLLOW_UP',
    'MANAGE',
    'Manage bounded internal Service Profit follow-up handling within authorized scope.',
    TRUE,
    TRUE
);