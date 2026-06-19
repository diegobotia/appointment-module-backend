-- =============================================
-- V2: CREACIÓN DE CITAS MÉDICAS (APPOINTMENTS)
-- =============================================

-- Tabla: appointments (Citas Médicas)
CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    schedule_id UUID REFERENCES schedules(id),
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 30,
    appointment_type VARCHAR(20) NOT NULL CHECK (appointment_type IN ('PRESENCIAL', 'TELEMEDICINA')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    
    CONSTRAINT chk_future_appointment CHECK (
        appointment_date >= CURRENT_DATE OR 
        (appointment_date = CURRENT_DATE AND appointment_time >= CURRENT_TIME)
    )
);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_date_status ON appointments(appointment_date, status);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);

-- =============================================
-- FUNCIONES Y TRIGGERS PARA UPDATED_AT
-- =============================================

DROP TRIGGER IF EXISTS trg_appointments_updated_at ON appointments;
CREATE TRIGGER trg_appointments_updated_at
    BEFORE UPDATE ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- VISTAS ÚTILES
-- =============================================

-- Vista: Citas con información enriquecida
DROP VIEW IF EXISTS v_appointments_enriched CASCADE;
CREATE OR REPLACE VIEW v_appointments_enriched AS
SELECT 
    a.id,
    a.patient_id,
    a.doctor_id,
    a.appointment_date,
    a.appointment_time,
    a.duration_minutes,
    a.appointment_type,
    a.status,
    a.reason,
    a.created_at,
    s.specialty,
    s.facility_id,
    CASE 
        WHEN a.status IN ('SCHEDULED', 'CONFIRMED') 
            AND a.appointment_date = CURRENT_DATE 
            AND a.appointment_time <= CURRENT_TIME + INTERVAL '30 minutes'
        THEN true
        ELSE false
    END AS is_upcoming_soon
FROM appointments a
LEFT JOIN schedules s ON a.schedule_id = s.id;
