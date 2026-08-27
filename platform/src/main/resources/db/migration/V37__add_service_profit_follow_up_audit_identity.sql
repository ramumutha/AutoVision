ALTER TABLE platform.service_profit_follow_up_history
    ALTER COLUMN actor_principal_id DROP NOT NULL;

ALTER TABLE platform.service_profit_follow_up_history
    ADD COLUMN actor_type VARCHAR(16),
    ADD COLUMN application_id VARCHAR(120);

UPDATE platform.service_profit_follow_up_history
   SET actor_type = 'HUMAN'
 WHERE actor_type IS NULL;

ALTER TABLE platform.service_profit_follow_up_history
    ALTER COLUMN actor_type SET NOT NULL;

ALTER TABLE platform.service_profit_follow_up_history
    ADD CONSTRAINT ck_service_profit_follow_up_history_actor_type
        CHECK (actor_type IN ('HUMAN', 'SYSTEM')),
    ADD CONSTRAINT ck_service_profit_follow_up_history_actor_identity
        CHECK (
            (actor_type = 'HUMAN'
             AND actor_principal_id IS NOT NULL
             AND application_id IS NULL)
            OR
            (actor_type = 'SYSTEM'
             AND actor_principal_id IS NULL
             AND application_id = 'AUTOVISION_SERVICE_PROFIT')
        );