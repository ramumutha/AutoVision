-- AutoVision Service Profit AI R1
-- Dealer data readiness assessment foundation.
--
-- Stores explainable assessment outcomes.
-- Raw dealer files, staging records, mapping definitions and ingestion
-- pipelines remain outside the R1.3.1 boundary.

CREATE TABLE platform.dealer_data_assessments (
    id UUID PRIMARY KEY,

    tenant_id UUID NOT NULL,
    dealer_id UUID,
    branch_id UUID,

    source_name VARCHAR(255) NOT NULL,
    source_system VARCHAR(100),

    status VARCHAR(30) NOT NULL,

    identity_coverage NUMERIC(5,2) NOT NULL,
    vehicle_linkage_coverage NUMERIC(5,2) NOT NULL,
    service_transaction_coverage NUMERIC(5,2) NOT NULL,
    recommendation_evidence_coverage NUMERIC(5,2) NOT NULL,
    disposition_coverage NUMERIC(5,2) NOT NULL,
    mileage_coverage NUMERIC(5,2) NOT NULL,
    invoice_linkage_coverage NUMERIC(5,2) NOT NULL,
    cost_coverage NUMERIC(5,2) NOT NULL,

    overall_score NUMERIC(5,2) NOT NULL,
    assessment_policy_version VARCHAR(100) NOT NULL,

    assessed_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    created_by_principal_id UUID NOT NULL,
    updated_by_principal_id UUID NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT ck_dealer_data_assessment_status
        CHECK (status IN ('DRAFT', 'COMPLETED')),

    CONSTRAINT ck_dealer_data_identity_coverage
        CHECK (identity_coverage BETWEEN 0 AND 100),

    CONSTRAINT ck_dealer_data_vehicle_linkage_coverage
        CHECK (vehicle_linkage_coverage BETWEEN 0 AND 100),

    CONSTRAINT ck_dealer_data_service_transaction_coverage
        CHECK (service_transaction_coverage BETWEEN 0 AND 100),

    CONSTRAINT ck_dealer_data_recommendation_evidence_coverage
        CHECK (recommendation_evidence_coverage BETWEEN 0 AND 100),

    CONSTRAINT ck_dealer_data_disposition_coverage
        CHECK (disposition_coverage BETWEEN 0 AND 100),

    CONSTRAINT ck_dealer_data_mileage_coverage
        CHECK (mileage_coverage BETWEEN 0 AND 100),

    CONSTRAINT ck_dealer_data_invoice_linkage_coverage
        CHECK (invoice_linkage_coverage BETWEEN 0 AND 100),

    CONSTRAINT ck_dealer_data_cost_coverage
        CHECK (cost_coverage BETWEEN 0 AND 100),

    CONSTRAINT ck_dealer_data_overall_score
        CHECK (overall_score BETWEEN 0 AND 100),

    CONSTRAINT fk_dealer_data_assessment_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES public.tenants(id),

    CONSTRAINT fk_dealer_data_assessment_dealer
        FOREIGN KEY (dealer_id)
        REFERENCES platform.dealers(id),

    CONSTRAINT fk_dealer_data_assessment_branch
        FOREIGN KEY (branch_id)
        REFERENCES platform.branches(id),

    CONSTRAINT ck_dealer_data_assessment_branch_requires_dealer
        CHECK (branch_id IS NULL OR dealer_id IS NOT NULL)
);

CREATE TABLE platform.dealer_data_capability_assessments (
    id UUID PRIMARY KEY,

    assessment_id UUID NOT NULL,

    capability VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(1000) NOT NULL,

    created_by_principal_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_dealer_data_capability_assessment
        FOREIGN KEY (assessment_id)
        REFERENCES platform.dealer_data_assessments(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_dealer_data_capability_assessment
        UNIQUE (assessment_id, capability),

    CONSTRAINT ck_dealer_data_capability
        CHECK (
            capability IN (
                'DECLINED_WORK_EXPLICIT',
                'DECLINED_WORK_RECONSTRUCTION',
                'DEFERRED_WORK',
                'DUE_OVERDUE_SERVICE',
                'INACTIVE_CUSTOMER',
                'REVENUE_ATTRIBUTION',
                'GROSS_PROFIT_ATTRIBUTION'
            )
        ),

    CONSTRAINT ck_dealer_data_capability_status
        CHECK (
            status IN (
                'AVAILABLE',
                'PARTIAL',
                'UNAVAILABLE'
            )
        )
);

CREATE INDEX idx_dealer_data_assessment_tenant_created
    ON platform.dealer_data_assessments (
        tenant_id,
        created_at DESC
    );

CREATE INDEX idx_dealer_data_assessment_tenant_dealer
    ON platform.dealer_data_assessments (
        tenant_id,
        dealer_id,
        created_at DESC
    );

CREATE INDEX idx_dealer_data_capability_assessment_parent
    ON platform.dealer_data_capability_assessments (
        assessment_id
    );

