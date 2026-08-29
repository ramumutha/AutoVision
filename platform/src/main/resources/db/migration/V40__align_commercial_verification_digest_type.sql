ALTER TABLE platform.commercial_verifications
    ALTER COLUMN token_digest TYPE VARCHAR(64)
    USING token_digest::VARCHAR;
