-- AutoVision Service Profit AI R1.3.3
-- Adds durable source dataset identity for idempotent dealer-data assessment loading.

ALTER TABLE platform.dealer_data_assessments
    ADD COLUMN source_dataset_id VARCHAR(160),
    ADD COLUMN source_dataset_version VARCHAR(80);

ALTER TABLE platform.dealer_data_assessments
    ADD CONSTRAINT ck_dealer_data_assessment_source_identity_pair
        CHECK (
            (
                source_dataset_id IS NULL
                AND source_dataset_version IS NULL
            )
            OR
            (
                source_dataset_id IS NOT NULL
                AND source_dataset_version IS NOT NULL
            )
        );

ALTER TABLE platform.dealer_data_assessments
    ADD CONSTRAINT uq_dealer_data_assessment_tenant_dataset_version
        UNIQUE (
            tenant_id,
            source_dataset_id,
            source_dataset_version
        );
