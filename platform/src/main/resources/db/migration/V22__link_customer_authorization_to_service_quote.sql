-- Sprint 5 S5.7.1-A: optional CustomerAuthorization to ServiceQuote linkage.
-- The link is tenant- and AfterSalesCase-contained; quote and authorization
-- lifecycles remain independent.

ALTER TABLE platform.service_quotes
    ADD CONSTRAINT uq_service_quotes_id_tenant_case
        UNIQUE (id, tenant_id, after_sales_case_id);

ALTER TABLE platform.customer_authorizations
    ADD COLUMN service_quote_id UUID;

ALTER TABLE platform.customer_authorizations
    ADD CONSTRAINT fk_customer_authorizations_service_quote
        FOREIGN KEY (service_quote_id, tenant_id, aftersales_case_id)
        REFERENCES platform.service_quotes(
            id,
            tenant_id,
            after_sales_case_id
        )
        ON DELETE RESTRICT;

CREATE INDEX ix_customer_authorizations_service_quote_id
    ON platform.customer_authorizations(service_quote_id)
    WHERE service_quote_id IS NOT NULL;
