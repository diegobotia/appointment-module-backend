-- Alinear notifications con esquema Supabase (V12) y soporte de reintentos

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'notifications'
          AND column_name = 'external_entity_id'
    ) THEN
        ALTER TABLE appointments.notifications RENAME COLUMN external_entity_id TO entity_id;
    END IF;
END $$;

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
