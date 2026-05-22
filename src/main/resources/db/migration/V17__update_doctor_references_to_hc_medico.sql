-- =================================================================
-- V17: ACTUALIZAR REFERENCIAS DE MEDICOS A HC.MEDICO
-- Fecha: 18 de mayo de 2026
-- Descripción: Cambia las referencias de appointments.* hacia hc.medico
--              en lugar de tablas locales o core.profiles.
--              Bootstrap de hc.medico incluido aquí (antes de V21 en orden Flyway).
-- =================================================================

-- ============================================================
-- 0. BOOTSTRAP HC.MEDICO (requerido en local antes de FKs)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS hc;

CREATE TABLE IF NOT EXISTS hc.medico (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_medico VARCHAR(50) UNIQUE NOT NULL,
    primer_nombre VARCHAR(100) NOT NULL,
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    segundo_apellido VARCHAR(100),
    email VARCHAR(120) UNIQUE,
    telefono VARCHAR(30),
    especialidad_principal VARCHAR(100),
    numero_registro_medico VARCHAR(100) UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT hc_medico_email_valid CHECK (email IS NULL OR email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

CREATE INDEX IF NOT EXISTS idx_hc_medico_numero_medico ON hc.medico(numero_medico);
CREATE INDEX IF NOT EXISTS idx_hc_medico_email ON hc.medico(email);
CREATE INDEX IF NOT EXISTS idx_hc_medico_especialidad ON hc.medico(especialidad_principal);
CREATE INDEX IF NOT EXISTS idx_hc_medico_activo ON hc.medico(activo);

INSERT INTO hc.medico (
    numero_medico,
    primer_nombre,
    primer_apellido,
    email,
    especialidad_principal,
    activo
)
VALUES
    ('MED001', 'Carlos', 'Rodriguez', 'carlos.rodriguez@ips.test', 'Fisiatria', true),
    ('MED002', 'Ana', 'Martinez', 'ana.martinez@ips.test', 'Dolor', true),
    ('MED003', 'Roberto', 'Lopez', 'roberto.lopez@ips.test', 'Medicina Laboral', true),
    ('MED004', 'Maria', 'Garcia', 'maria.garcia@ips.test', 'Psicologia', true),
    ('MED005', 'Juan', 'Hernandez', 'juan.hernandez@ips.test', 'Psiquiatria', true)
ON CONFLICT (numero_medico) DO NOTHING;

-- ============================================================
-- 1. REMOVER CONSTRAINT EXISTENTES A core.profiles (V12/V13)
-- ============================================================

ALTER TABLE IF EXISTS appointments.schedules DROP CONSTRAINT IF EXISTS schedules_doctor_id_fkey;
ALTER TABLE IF EXISTS appointments.schedules DROP CONSTRAINT IF EXISTS fk_schedules_doctor;

ALTER TABLE IF EXISTS appointments.appointments DROP CONSTRAINT IF EXISTS appointments_doctor_id_fkey;
ALTER TABLE IF EXISTS appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_doctor;
ALTER TABLE IF EXISTS appointments.appointments DROP CONSTRAINT IF EXISTS appointments_secondary_doctor_id_fkey;

ALTER TABLE IF EXISTS appointments.appointment_participants DROP CONSTRAINT IF EXISTS appointment_participants_doctor_id_fkey;
ALTER TABLE IF EXISTS appointments.appointment_participants DROP CONSTRAINT IF EXISTS fk_appointment_participants_doctor;

ALTER TABLE IF EXISTS appointments.schedule_plans DROP CONSTRAINT IF EXISTS schedule_plans_specialist_id_fkey;
ALTER TABLE IF EXISTS appointments.schedule_plans DROP CONSTRAINT IF EXISTS fk_schedule_plans_specialist;

-- ============================================================
-- 2. doctor_id VARCHAR → hc.medicos.id (sin FK en BD; ver V12/V13)
-- La aplicación Java usa String doctorId alineado a hc.medicos.
-- hc.medico (singular) queda como bootstrap local opcional; prod usa hc.medicos.
-- ============================================================

-- ============================================================
-- 3. RECREAR ÍNDICES PARA PERFORMANCE
-- ============================================================

DROP INDEX IF EXISTS idx_schedules_doctor_day;
CREATE INDEX IF NOT EXISTS idx_schedules_doctor_day ON appointments.schedules(doctor_id, day_of_week);

DROP INDEX IF EXISTS idx_appointment_participants_doctor;
CREATE INDEX IF NOT EXISTS idx_appointment_participants_doctor
    ON appointments.appointment_participants(doctor_id, appointment_id);

DROP INDEX IF EXISTS idx_schedule_plans_specialist;
CREATE INDEX IF NOT EXISTS idx_schedule_plans_specialist
    ON appointments.schedule_plans(specialist_id, plan_year, plan_quarter);
