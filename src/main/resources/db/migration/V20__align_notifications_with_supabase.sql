-- Alinear notifications con esquema Supabase (V12) y soporte de reintentos
-- Idempotente: prod puede tener entity_id (V12), external_entity_id (legacy) o ambas.

DO $v20$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'notifications'
          AND column_name = 'external_entity_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'notifications'
          AND column_name = 'entity_id'
    ) THEN
        ALTER TABLE appointments.notifications
            RENAME COLUMN external_entity_id TO entity_id;
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'notifications'
          AND column_name = 'external_entity_id'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'notifications'
          AND column_name = 'entity_id'
    ) THEN
        UPDATE appointments.notifications
        SET entity_id = COALESCE(entity_id, external_entity_id)
        WHERE external_entity_id IS NOT NULL;

        ALTER TABLE appointments.notifications
            DROP COLUMN external_entity_id;
    END IF;
END
$v20$;

ALTER TABLE appointments.notifications
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50) NOT NULL DEFAULT 'APPOINTMENT';

ALTER TABLE appointments.notifications
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE appointments.notifications
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP;

ALTER TABLE appointments.notifications
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(50) NOT NULL DEFAULT 'APPOINTMENT_CREATED';

UPDATE appointments.notifications
SET entity_type = 'APPOINTMENT'
WHERE entity_type IS NULL OR entity_type = '';

CREATE INDEX IF NOT EXISTS idx_notifications_entity
    ON appointments.notifications(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_notifications_pending
    ON appointments.notifications(status, created_at ASC)
    WHERE status IN ('PENDING', 'FAILED');
