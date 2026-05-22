-- =================================================================
-- V12: BASELINE SUPABASE - SCHEMA APPOINTMENTS
-- Fecha: 2026-05-16
-- Descripción: Refactorización completa para Supabase multi-schema
--   - Schema appointments contiene solo operaciones de citas
--   - Referencias: core.pacientes (UUID), hc.medicos (doctor_id VARCHAR)
--   - Tabla temporal specialist_metadata para resolver cuello de botella hc
-- =================================================================

-- ================================================================
-- 1. CREAR SCHEMA APPOINTMENTS (si no existe)
-- ================================================================
CREATE SCHEMA IF NOT EXISTS appointments;

-- ================================================================
-- 2. RECREAR TABLAS PRINCIPALES CON REFERENCIAS CORRECTAS
-- ================================================================

-- 2.1 SCHEDULES (Agendas Médicas)
CREATE TABLE IF NOT EXISTS appointments.schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id VARCHAR(64) NOT NULL,
    facility_id UUID NOT NULL,  -- Se asume que facilities ya existe en appointments
    specialty VARCHAR(100),
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 30,
    max_patients_per_slot INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_schedules_time_range CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_schedules_doctor_day ON appointments.schedules(doctor_id, day_of_week);
CREATE INDEX IF NOT EXISTS idx_schedules_facility ON appointments.schedules(facility_id);
CREATE INDEX IF NOT EXISTS idx_schedules_doctor_facility_day ON appointments.schedules(doctor_id, facility_id, day_of_week);

-- 2.2 SCHEDULE_BLOCKS (Bloqueos de Agenda)
CREATE TABLE IF NOT EXISTS appointments.schedule_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL REFERENCES appointments.schedules(id) ON DELETE CASCADE,
    block_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(200),
    created_by UUID REFERENCES core.profiles(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_schedule_blocks_time_range CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_schedule_blocks_schedule_date ON appointments.schedule_blocks(schedule_id, block_date);

-- ================================================================
-- 3. TABLA PRINCIPAL: APPOINTMENTS (Citas Médicas)
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES core.pacientes(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    doctor_id VARCHAR(64) NOT NULL,
    schedule_id UUID REFERENCES appointments.schedules(id),
    facility_id UUID NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 30,
    appointment_type VARCHAR(50) NOT NULL CHECK (appointment_type IN (
        'PRESENCIAL',
        'TELEMEDICINA',
        'JUNTA_MEDICA',
        'TERAPIA_FISICA',
        'TERAPIA_OCUPACIONAL'
    )),
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN (
        'SCHEDULED',
        'PENDIENTE_CONFIRMACION_GRUPO',
        'CONFIRMED',
        'CHECKED_IN',
        'COMPLETED',
        'CANCELLED',
        'NO_SHOW'
    )),
    specialty VARCHAR(100),
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    
    CONSTRAINT chk_appointments_future CHECK (
        appointment_date > CURRENT_DATE OR 
        (appointment_date = CURRENT_DATE AND appointment_time > CURRENT_TIME)
    )
);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments.appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date ON appointments.appointments(doctor_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_date_status ON appointments.appointments(appointment_date, status);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments.appointments(status);
CREATE INDEX IF NOT EXISTS idx_appointments_facility_date_status ON appointments.appointments(facility_id, appointment_date, status);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_date_status ON appointments.appointments(patient_id, appointment_date DESC, status);
CREATE INDEX IF NOT EXISTS idx_appointments_specialist_available ON appointments.appointments(doctor_id, appointment_date, status) WHERE status IN ('SCHEDULED', 'CONFIRMED');
CREATE INDEX IF NOT EXISTS idx_appointments_type_date ON appointments.appointments(appointment_type, appointment_date DESC) WHERE status != 'CANCELLED';

-- ================================================================
-- 4. TABLA TEMPORAL: SPECIALIST_METADATA
-- Almacena metadata del médico mientras hc.specialties no está lista
-- Será deprecada cuando hc entregue su tabla de especialidades
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.specialist_metadata (
    profile_id UUID PRIMARY KEY REFERENCES core.profiles(id) ON DELETE CASCADE,
    specialties_json JSONB NOT NULL DEFAULT '{"primary": null, "secondary": [], "available": []}',
    max_patients_per_slot INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    synced_from_hc BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_specialist_metadata_max_patients CHECK (max_patients_per_slot BETWEEN 1 AND 10)
);

CREATE INDEX IF NOT EXISTS idx_specialist_metadata_active ON appointments.specialist_metadata(is_active);
CREATE INDEX IF NOT EXISTS idx_specialist_metadata_synced ON appointments.specialist_metadata(synced_from_hc);

-- ================================================================
-- 5. APPOINTMENT_PARTICIPANTS (Para Juntas Médicas)
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.appointment_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments.appointments(id) ON DELETE CASCADE,
    doctor_id VARCHAR(64) NOT NULL,
    participant_order INTEGER NOT NULL CHECK (participant_order BETWEEN 1 AND 10),
    participant_role VARCHAR(20) NOT NULL CHECK (participant_role IN ('PRIMARY', 'SECONDARY')),
    confirmed_at TIMESTAMP,
    
    CONSTRAINT uq_appointment_participants_order UNIQUE (appointment_id, participant_order),
    CONSTRAINT uq_appointment_participants_doctor UNIQUE (appointment_id, doctor_id)
);

CREATE INDEX IF NOT EXISTS idx_appointment_participants_doctor_date ON appointments.appointment_participants(doctor_id, appointment_id);
CREATE INDEX IF NOT EXISTS idx_appointment_participants_appointment ON appointments.appointment_participants(appointment_id);

-- ================================================================
-- 6. FACILITIES (Sedes)
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_facilities_active ON appointments.facilities(is_active);

INSERT INTO appointments.facilities (code, name, address, is_active)
VALUES
    ('SEDE_PRINCIPAL', 'Sede Principal', 'Dirección pendiente - Sede Principal', true),
    ('SEDE_NORTE', 'Sede Norte', 'Dirección pendiente - Sede Norte', true)
ON CONFLICT (code) DO NOTHING;

-- Asegurar integridad referencial: añadir FK desde schedules.facility_id y appointments.facility_id
ALTER TABLE IF EXISTS appointments.schedules
    DROP CONSTRAINT IF EXISTS fk_schedules_facility;

ALTER TABLE IF EXISTS appointments.schedules
    ADD CONSTRAINT fk_schedules_facility
    FOREIGN KEY (facility_id) REFERENCES appointments.facilities(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE IF EXISTS appointments.appointments
    DROP CONSTRAINT IF EXISTS fk_appointments_facility;

ALTER TABLE IF EXISTS appointments.appointments
    ADD CONSTRAINT fk_appointments_facility
    FOREIGN KEY (facility_id) REFERENCES appointments.facilities(id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- ================================================================
-- 7. NOTIFICATIONS (Notificaciones)
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL DEFAULT 'APPOINTMENT',
    entity_id UUID NOT NULL,
    notification_type VARCHAR(20) NOT NULL CHECK (notification_type IN ('SMS', 'WHATSAPP', 'EMAIL')),
    recipient VARCHAR(120) NOT NULL,
    message_content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    sent_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_entity ON appointments.notifications(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status_created_at ON appointments.notifications(status, created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_pending ON appointments.notifications(status, created_at ASC) WHERE status IN ('PENDING', 'FAILED');

-- ================================================================
-- 8. DOMAIN_EVENTS (Event Sourcing para n8n)
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.domain_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_data JSONB NOT NULL,
    occurred_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_domain_events_aggregate ON appointments.domain_events(aggregate_id, occurred_on);
CREATE INDEX IF NOT EXISTS idx_domain_events_type ON appointments.domain_events(event_type);
CREATE INDEX IF NOT EXISTS idx_domain_events_unpublished ON appointments.domain_events(published) WHERE published = false;

-- ================================================================
-- 9. AUDITORÍA: APPOINTMENT_STATE_HISTORY
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.appointment_state_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments.appointments(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID REFERENCES core.profiles(id),
    reason VARCHAR(500),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_appointment_state_history_appointment ON appointments.appointment_state_history(appointment_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_appointment_state_history_user ON appointments.appointment_state_history(changed_by, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_appointment_state_history_changed_at ON appointments.appointment_state_history(changed_at DESC);

-- ================================================================
-- 10. AUDITORÍA: APPOINTMENT_ADMISSION_LINK
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.appointment_admissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments.appointments(id) ON DELETE CASCADE,
    admission_id UUID NOT NULL REFERENCES core.admisiones(id) ON DELETE RESTRICT,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    linked_by UUID REFERENCES core.profiles(id),
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_appointment_admissions_admission ON appointments.appointment_admissions(admission_id);

-- ================================================================
-- 11. AUDITORÍA: APPOINTMENT_AUDIT_LOG
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.appointment_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments.appointments(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by UUID REFERENCES core.profiles(id),
    ip_address VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_appointment_audit_log_appointment ON appointments.appointment_audit_log(appointment_id, performed_at DESC);
CREATE INDEX IF NOT EXISTS idx_appointment_audit_log_performed_at ON appointments.appointment_audit_log(performed_at DESC);

-- ================================================================
-- 12. SCHEDULE_PLANS (Planificación Trimestral)
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.schedule_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    specialist_id VARCHAR(64) NOT NULL,
    plan_year INTEGER NOT NULL,
    plan_quarter INTEGER NOT NULL CHECK (plan_quarter BETWEEN 1 AND 4),
    version_number INTEGER NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT false,
    is_active_version BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_schedule_plan_version UNIQUE (specialist_id, plan_year, plan_quarter, version_number)
);

CREATE INDEX IF NOT EXISTS idx_schedule_plans_specialist_period ON appointments.schedule_plans(specialist_id, plan_year, plan_quarter);
CREATE UNIQUE INDEX IF NOT EXISTS uq_schedule_plan_active_period ON appointments.schedule_plans(specialist_id, plan_year, plan_quarter) WHERE is_active_version = true;

-- ================================================================
-- 13. SCHEDULE_PLAN_SLOTS
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.schedule_plan_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_plan_id UUID NOT NULL REFERENCES appointments.schedule_plans(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL,
    max_patients_per_slot INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_plan_slot_time_range CHECK (end_time > start_time),
    CONSTRAINT chk_schedule_plan_slot_duration CHECK (slot_duration_minutes > 0),
    CONSTRAINT chk_schedule_plan_slot_capacity CHECK (max_patients_per_slot > 0)
);

CREATE INDEX IF NOT EXISTS idx_schedule_plan_slots_plan ON appointments.schedule_plan_slots(schedule_plan_id, day_of_week);

-- ================================================================
-- 14. SCHEDULE_PLAN_BLOCKS
-- ================================================================

CREATE TABLE IF NOT EXISTS appointments.schedule_plan_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_plan_id UUID NOT NULL REFERENCES appointments.schedule_plans(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(255),
    created_by UUID REFERENCES core.profiles(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_plan_block_date_range CHECK (end_date >= start_date),
    CONSTRAINT chk_schedule_plan_block_time_range CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_schedule_plan_blocks_plan_date ON appointments.schedule_plan_blocks(schedule_plan_id, start_date, end_date);

-- ================================================================
-- 15. FUNCIONES Y TRIGGERS
-- ================================================================

-- Función para actualizar updated_at
CREATE OR REPLACE FUNCTION appointments.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para schedules
DROP TRIGGER IF EXISTS trg_schedules_updated_at ON appointments.schedules;
CREATE TRIGGER trg_schedules_updated_at
    BEFORE UPDATE ON appointments.schedules
    FOR EACH ROW
    EXECUTE FUNCTION appointments.update_updated_at_column();

-- Trigger para appointments
DROP TRIGGER IF EXISTS trg_appointments_updated_at ON appointments.appointments;
CREATE TRIGGER trg_appointments_updated_at
    BEFORE UPDATE ON appointments.appointments
    FOR EACH ROW
    EXECUTE FUNCTION appointments.update_updated_at_column();

-- Trigger para registrar cambios de estado
CREATE OR REPLACE FUNCTION appointments.log_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO appointments.appointment_state_history 
        (appointment_id, old_status, new_status, changed_at)
        VALUES (NEW.id, OLD.status, NEW.status, CURRENT_TIMESTAMP);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_log_status_change ON appointments.appointments;
CREATE TRIGGER trg_log_status_change
    AFTER UPDATE ON appointments.appointments
    FOR EACH ROW
    EXECUTE FUNCTION appointments.log_status_change();

-- Trigger para validar coherencia facility
CREATE OR REPLACE FUNCTION appointments.validate_appointment_facility()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.schedule_id IS NOT NULL THEN
        IF NEW.facility_id IS DISTINCT FROM (
            SELECT facility_id FROM appointments.schedules WHERE id = NEW.schedule_id
        ) THEN
            RAISE EXCEPTION 'Facility mismatch between appointment and schedule';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_facility ON appointments.appointments;
CREATE TRIGGER trg_validate_facility
    BEFORE INSERT OR UPDATE ON appointments.appointments
    FOR EACH ROW
    EXECUTE FUNCTION appointments.validate_appointment_facility();

-- Trigger para facilities
DROP TRIGGER IF EXISTS trg_facilities_updated_at ON appointments.facilities;
CREATE TRIGGER trg_facilities_updated_at
    BEFORE UPDATE ON appointments.facilities
    FOR EACH ROW
    EXECUTE FUNCTION appointments.update_updated_at_column();

-- Trigger para specialist_metadata
DROP TRIGGER IF EXISTS trg_specialist_metadata_updated_at ON appointments.specialist_metadata;
CREATE TRIGGER trg_specialist_metadata_updated_at
    BEFORE UPDATE ON appointments.specialist_metadata
    FOR EACH ROW
    EXECUTE FUNCTION appointments.update_updated_at_column();

-- Trigger para schedule_plans
DROP TRIGGER IF EXISTS trg_schedule_plans_updated_at ON appointments.schedule_plans;
CREATE TRIGGER trg_schedule_plans_updated_at
    BEFORE UPDATE ON appointments.schedule_plans
    FOR EACH ROW
    EXECUTE FUNCTION appointments.update_updated_at_column();

-- ================================================================
-- 15b. Quitar FKs erróneas de doctor_id hacia core.profiles (legacy Supabase)
-- doctor_id permanece VARCHAR; referencia lógica a hc.medicos.id
-- ================================================================

ALTER TABLE IF EXISTS appointments.schedules
    DROP CONSTRAINT IF EXISTS schedules_doctor_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_schedules_doctor;

ALTER TABLE IF EXISTS appointments.appointments
    DROP CONSTRAINT IF EXISTS appointments_doctor_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_appointments_doctor;

ALTER TABLE IF EXISTS appointments.appointment_participants
    DROP CONSTRAINT IF EXISTS appointment_participants_doctor_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_appointment_participants_doctor;

ALTER TABLE IF EXISTS appointments.schedule_plans
    DROP CONSTRAINT IF EXISTS schedule_plans_specialist_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_schedule_plans_specialist;

DROP FUNCTION IF EXISTS appointments.resolve_medico_uuid(TEXT);

-- ================================================================
-- 16. VISTAS ÚTILES
-- ================================================================

CREATE OR REPLACE VIEW appointments.v_appointments_complete AS
SELECT 
    a.id,
    a.patient_id,
    p.nombres || ' ' || p.apellidos AS patient_name,
    p.num_identificacion AS patient_id_number,
    a.doctor_id,
    COALESCE(TRIM(med.nombre || ' ' || med.apellido), 'Sin asignar') AS doctor_name,
    med.registro AS doctor_registro,
    a.facility_id,
    f.name AS facility_name,
    a.appointment_date,
    a.appointment_time,
    a.duration_minutes,
    a.appointment_type,
    a.status,
    a.specialty,
    a.reason,
    a.created_at,
    (SELECT new_status FROM appointments.appointment_state_history 
     WHERE appointment_id = a.id 
     ORDER BY changed_at DESC LIMIT 1) AS current_status,
    (SELECT changed_at FROM appointments.appointment_state_history 
     WHERE appointment_id = a.id 
     ORDER BY changed_at DESC LIMIT 1) AS last_status_change
FROM appointments.appointments a
LEFT JOIN core.pacientes p ON a.patient_id = p.id
LEFT JOIN hc.medicos med ON med.id::text = a.doctor_id::text
LEFT JOIN appointments.facilities f ON a.facility_id = f.id;

-- Vista para especialista ver sus citas disponibles
CREATE OR REPLACE VIEW appointments.v_doctor_available_slots AS
SELECT 
    s.id as schedule_id,
    s.doctor_id,
    COALESCE(TRIM(med.nombre || ' ' || med.apellido), 'Sin asignar') as doctor_name,
    s.facility_id,
    f.name as facility_name,
    s.day_of_week,
    s.start_time,
    s.end_time,
    s.slot_duration_minutes,
    s.max_patients_per_slot,
    sm.max_patients_per_slot as specialist_max_capacity,
    (SELECT COUNT(*) FROM appointments.appointments 
     WHERE schedule_id = s.id 
     AND status IN ('SCHEDULED', 'CONFIRMED')) as appointments_count
FROM appointments.schedules s
LEFT JOIN hc.medicos med ON med.id::text = s.doctor_id::text
JOIN appointments.facilities f ON s.facility_id = f.id
LEFT JOIN appointments.specialist_metadata sm ON sm.profile_id::text = s.doctor_id::text
WHERE s.is_active = true;

-- ================================================================
-- FIN V12
-- ================================================================
-- Esta migración establece el baseline para Supabase con:
-- ✅ Schema appointments separado
-- ✅ Referencias: core.pacientes + hc.medicos (doctor_id VARCHAR)
-- ✅ Tabla temporal specialist_metadata para resolver hc
-- ✅ Auditoría completa
-- ✅ Índices para performance
-- ✅ Triggers para integridad
-- ✅ Vistas para consultas comunes
-- =================================================================
