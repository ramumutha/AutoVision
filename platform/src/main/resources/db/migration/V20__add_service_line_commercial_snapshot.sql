-- Sprint 5 S5.5.1-D1 ServiceLine commercial snapshot foundation.
-- Existing operational lines remain valid without commercial values.

ALTER TABLE platform.service_lines
    ADD COLUMN unit_price NUMERIC(19, 4),
    ADD COLUMN currency_code VARCHAR(3),
    ADD COLUMN net_amount NUMERIC(19, 4),
    ADD COLUMN tax_amount NUMERIC(19, 4),
    ADD COLUMN gross_amount NUMERIC(19, 4);

ALTER TABLE platform.service_lines
    ADD CONSTRAINT ck_service_lines_unit_price
        CHECK (unit_price IS NULL OR unit_price >= 0),
    ADD CONSTRAINT ck_service_lines_net_amount
        CHECK (net_amount IS NULL OR net_amount >= 0),
    ADD CONSTRAINT ck_service_lines_tax_amount
        CHECK (tax_amount IS NULL OR tax_amount >= 0),
    ADD CONSTRAINT ck_service_lines_gross_amount
        CHECK (gross_amount IS NULL OR gross_amount >= 0);