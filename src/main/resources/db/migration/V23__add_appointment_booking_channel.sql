ALTER TABLE appointments.appointments
    ADD COLUMN IF NOT EXISTS booking_channel VARCHAR(20) NOT NULL DEFAULT 'STAFF';

ALTER TABLE appointments.appointments
    ADD COLUMN IF NOT EXISTS n8n_conversation_id VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_appointments_booking_channel
    ON appointments.appointments(booking_channel, created_at DESC);
