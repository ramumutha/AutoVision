-- Sprint 5 AfterSales authorization permission catalog.
--
-- Registers platform-defined permission capabilities required by the
-- AfterSalesCase and CustomerAuthorization domain services.
--
-- This migration deliberately does not assign permissions to roles,
-- permission sets, principals, tenants, dealers or branches.
-- Authorization assignment remains configurable through the existing
-- authorization model and must follow least-privilege policy.
--
-- CustomerAuthorization permissions operate against the containing
-- AfterSalesCase authorization resource. No independent authorization
-- resource hierarchy is introduced for CustomerAuthorization.

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
        '84ef58d1-cab0-4c69-9c93-764a7800e101',
        'AFTERSALES_CASE.READ',
        'AFTERSALES_CASE',
        'READ',
        'Read an AfterSales case within the authorized organizational scope.',
        TRUE,
        TRUE
    ),
    (
        '84ef58d1-cab0-4c69-9c93-764a7800e102',
        'AFTERSALES_CASE.CREATE',
        'AFTERSALES_CASE',
        'CREATE',
        'Create an AfterSales case within the authorized organizational scope.',
        TRUE,
        TRUE
    ),
    (
        '84ef58d1-cab0-4c69-9c93-764a7800e103',
        'AFTERSALES_CASE.UPDATE',
        'AFTERSALES_CASE',
        'UPDATE',
        'Update the lifecycle of an AfterSales case within the authorized organizational scope.',
        TRUE,
        TRUE
    ),
    (
        '84ef58d1-cab0-4c69-9c93-764a7800e104',
        'CUSTOMER_AUTHORIZATION.READ',
        'CUSTOMER_AUTHORIZATION',
        'READ',
        'Read customer authorization records through their containing AfterSales case scope.',
        TRUE,
        TRUE
    ),
    (
        '84ef58d1-cab0-4c69-9c93-764a7800e105',
        'CUSTOMER_AUTHORIZATION.CREATE',
        'CUSTOMER_AUTHORIZATION',
        'CREATE',
        'Create customer authorization records through their containing AfterSales case scope.',
        TRUE,
        TRUE
    ),
    (
        '84ef58d1-cab0-4c69-9c93-764a7800e106',
        'CUSTOMER_AUTHORIZATION.DECIDE',
        'CUSTOMER_AUTHORIZATION',
        'DECIDE',
        'Record controlled customer authorization decisions through their containing AfterSales case scope.',
        TRUE,
        TRUE
    );