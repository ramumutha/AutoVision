ALTER TABLE platform.commercial_enquiries
    ADD COLUMN qualification JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE platform.commercial_enquiries
    ADD CONSTRAINT ck_commercial_enquiries_qualification_object
    CHECK (jsonb_typeof(qualification) = 'object');

COMMENT ON COLUMN platform.commercial_enquiries.qualification IS
    'Optional structured commercial qualification; legacy product and advisory columns remain authoritative for existing rows.';
