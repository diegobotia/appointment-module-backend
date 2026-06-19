-- =============================================
-- V3: CREACIÓN DE NOTIFICACIONES Y EVENT_LOG
-- =============================================

-- Tabla: notifications (Notificaciones)
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_entity_id UUID NOT NULL,
    notification_type VARCHAR(20) NOT NULL CHECK (notification_type IN ('SMS', 'WHATSAPP', 'EMAIL')),
    recipient VARCHAR(120) NOT NULL,
    message_content TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    sent_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_external_entity ON notifications(external_entity_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status_created_at ON notifications(status, created_at);

-- Tabla: domain_events (Event Sourcing Simplificado en BD)
CREATE TABLE IF NOT EXISTS domain_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_data JSONB NOT NULL,
    occurred_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published BOOLEAN DEFAULT false,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_domain_events_aggregate ON domain_events(aggregate_id, occurred_on);
CREATE INDEX IF NOT EXISTS idx_domain_events_type ON domain_events(event_type);
CREATE INDEX IF NOT EXISTS idx_domain_events_unpublished ON domain_events(published) WHERE published = false;
