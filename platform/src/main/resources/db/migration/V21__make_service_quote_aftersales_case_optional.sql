-- Sprint 5 S5.5.1-D1A: AfterSalesCase is optional quote provenance.
-- The existing tenant-contained foreign key remains in place; PostgreSQL
-- permits NULL foreign-key values while enforcing non-null references.

ALTER TABLE platform.service_quotes
    ALTER COLUMN after_sales_case_id DROP NOT NULL;