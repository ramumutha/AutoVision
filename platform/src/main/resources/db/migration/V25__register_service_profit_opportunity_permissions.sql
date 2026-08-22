-- AutoVision Service Profit AI R1.2.2 authorization capability.
--
-- Registers platform-defined permissions for controlled Service Profit
-- Opportunity creation and retrieval.
--
-- This migration deliberately does not assign permissions to roles,
-- permission sets, principals, tenants, dealers, branches, locations,
-- or groups. Authorization assignments remain configurable through the
-- existing least-privilege authorization model.

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
    'c72d2587-ec72-4a6d-ae94-850f438ed401',
    'SERVICE_PROFIT_OPPORTUNITY.CREATE',
    'SERVICE_PROFIT_OPPORTUNITY',
    'CREATE',
    'Create a Service Profit Opportunity within the authorized organizational scope.',
    TRUE,
    TRUE
),
(
    '8c221e8a-b08e-4dc1-976f-5f2f72f8ca02',
    'SERVICE_PROFIT_OPPORTUNITY.READ',
    'SERVICE_PROFIT_OPPORTUNITY',
    'READ',
    'Read a Service Profit Opportunity within the authorized organizational scope.',
    TRUE,
    TRUE
);
