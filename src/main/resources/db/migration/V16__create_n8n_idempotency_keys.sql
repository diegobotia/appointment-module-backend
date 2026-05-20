-- Idempotencia para flujos n8n (evitar doble reserva por conversation_id / request_id)
CREATE TABLE IF NOT EXISTS appointments.n8n_idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    appointment_id UUID REFERENCES appointments.appointments(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_n8n_idempotency_scope_key UNIQUE (scope, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_n8n_idempotency_appointment ON appointments.n8n_idempotency_keys(appointment_id);
