CREATE TABLE platform.commercial_enquiries (
    id UUID PRIMARY KEY,
    purpose VARCHAR(32) NOT NULL,
    company_name VARCHAR(160) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    business_email VARCHAR(254) NOT NULL,
    email_classification VARCHAR(24) NOT NULL,
    role_or_title VARCHAR(120) NOT NULL,
    country_or_market VARCHAR(80) NOT NULL,
    message_or_requirement VARCHAR(2000) NOT NULL,
    product_family VARCHAR(32),
    product VARCHAR(40),
    advisory_area VARCHAR(48),
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_commercial_enquiries_purpose CHECK (purpose IN ('PRODUCT_DEMO', 'ADVISORY_IMPLEMENTATION', 'PARTNERSHIP', 'GENERAL_ENQUIRY')),
    CONSTRAINT ck_commercial_enquiries_email_classification CHECK (email_classification IN ('BUSINESS_DOMAIN', 'FREE_MAIL', 'UNKNOWN')),
    CONSTRAINT ck_commercial_enquiries_status CHECK (status IN ('SUBMITTED', 'VERIFICATION_PENDING', 'VERIFIED', 'UNDER_REVIEW', 'QUALIFIED', 'CLOSED', 'REJECTED')),
    CONSTRAINT ck_commercial_enquiries_product CHECK (
        (purpose = 'PRODUCT_DEMO' AND product_family = 'AUTOVISION' AND product = 'SERVICE_PROFIT_AI')
        OR (purpose <> 'PRODUCT_DEMO' AND product_family IS NULL AND product IS NULL)
    ),
    CONSTRAINT ck_commercial_enquiries_advisory CHECK (
        (purpose = 'ADVISORY_IMPLEMENTATION' AND advisory_area IN ('AUTOMOTIVE_TECHNOLOGY_ADVISORY', 'AI_DIGITAL_TRANSFORMATION', 'INTEGRATION_IMPLEMENTATION', 'OTHER'))
        OR (purpose <> 'ADVISORY_IMPLEMENTATION' AND advisory_area IS NULL)
    ),
    CONSTRAINT ck_commercial_enquiries_text CHECK (
        length(trim(company_name)) > 0 AND length(trim(first_name)) > 0 AND length(trim(last_name)) > 0
        AND length(trim(business_email)) > 0 AND length(trim(role_or_title)) > 0
        AND length(trim(country_or_market)) > 0
    )
);

CREATE INDEX ix_commercial_enquiries_email_purpose_created
    ON platform.commercial_enquiries(business_email, purpose, product, created_at);

CREATE TABLE platform.commercial_verifications (
    id UUID PRIMARY KEY,
    enquiry_id UUID NOT NULL UNIQUE,
    token_digest CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_commercial_verifications_enquiry
        FOREIGN KEY (enquiry_id) REFERENCES platform.commercial_enquiries(id) ON DELETE CASCADE
);

CREATE INDEX ix_commercial_verifications_expires_at
    ON platform.commercial_verifications(expires_at);
