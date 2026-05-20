-- =================================================================
-- V13: Agregar Foreign Keys a core.pacientes y core.profiles
-- Fecha: 2026-05-16
-- Descripción: Refuerza integridad referencial con schemas externos
--
-- PRECONDICIONES:
-- - core.pacientes debe existir en schema 'core'
-- - core.profiles debe existir en schema 'core'
-- - core.roles debe existir en schema 'core'
-- - facilities table debe existir
--
-- CAMBIOS:
-- 1. schedules.doctor_id → core.profiles(id)
-- 2. appointments.patient_id → core.pacientes(id)
-- 3. appointments.doctor_id → core.profiles(id)
-- 4. appointments.facility_id → facilities(id)
-- 5. appointment_participants.doctor_id → core.profiles(id)
-- 6. schedule_plans.specialist_id → core.profiles(id)
-- 7. schedule_blocks.created_by → core.profiles(id)
-- 8. schedule_plan_blocks.created_by → core.profiles(id)
-- =================================================================

-- ========================
-- 1. SCHEDULES → CORE.PROFILES
-- ========================

ALTER TABLE appointments.schedules DROP CONSTRAINT IF EXISTS fk_schedules_doctor;
ALTER TABLE appointments.schedules
    ADD CONSTRAINT fk_schedules_doctor
    FOREIGN KEY (doctor_id) REFERENCES core.profiles(id) 
    ON DELETE RESTRICT ON UPDATE CASCADE;

CREATE INDEX IF NOT EXISTS idx_schedules_doctor_core 
    ON appointments.schedules(doctor_id);

-- ========================
-- 2. APPOINTMENTS → CORE
-- ========================

-- Validar que facility_id existe antes de agregar FK
ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_facility;
ALTER TABLE appointments.appointments
    ADD CONSTRAINT fk_appointments_facility
    FOREIGN KEY (facility_id) REFERENCES appointments.facilities(id) 
    ON DELETE RESTRICT;

-- FK a paciente
ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_patient;
ALTER TABLE appointments.appointments
    ADD CONSTRAINT fk_appointments_patient
    FOREIGN KEY (patient_id) REFERENCES core.pacientes(id) 
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- FK a médico
ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_doctor;
ALTER TABLE appointments.appointments
    ADD CONSTRAINT fk_appointments_doctor
    FOREIGN KEY (doctor_id) REFERENCES core.profiles(id) 
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- Mejorar CHECK para futuro más estricto
ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS chk_future_appointment;
ALTER TABLE appointments.appointments
    ADD CONSTRAINT chk_future_appointment
    CHECK (
        appointment_date > CURRENT_DATE OR 
        (appointment_date = CURRENT_DATE AND appointment_time > CURRENT_TIME)
    );

CREATE INDEX IF NOT EXISTS idx_appointments_patient_core 
    ON appointments.appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_core 
    ON appointments.appointments(doctor_id);

-- ========================
-- 3. APPOINTMENT_PARTICIPANTS → CORE.PROFILES
-- ========================

ALTER TABLE appointments.appointment_participants DROP CONSTRAINT IF EXISTS fk_appointment_participants_doctor;
ALTER TABLE appointments.appointment_participants
    ADD CONSTRAINT fk_appointment_participants_doctor
    FOREIGN KEY (doctor_id) REFERENCES core.profiles(id) 
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_appointment_participants_doctor_core 
    ON appointments.appointment_participants(doctor_id);

-- ========================
-- 4. SCHEDULE_PLANS → CORE.PROFILES
-- ========================

-- specialist_id ahora apunta a médico en core.profiles
ALTER TABLE appointments.schedule_plans DROP CONSTRAINT IF EXISTS fk_schedule_plans_specialist;
ALTER TABLE appointments.schedule_plans
    ADD CONSTRAINT fk_schedule_plans_specialist
    FOREIGN KEY (specialist_id) REFERENCES core.profiles(id) 
    ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_schedule_plans_specialist_core 
    ON appointments.schedule_plans(specialist_id);

-- ========================
-- 5. SCHEDULE_PLAN_BLOCKS → CORE.PROFILES (created_by)
-- ========================

ALTER TABLE appointments.schedule_plan_blocks DROP CONSTRAINT IF EXISTS fk_schedule_plan_blocks_created_by;
ALTER TABLE appointments.schedule_plan_blocks
    ADD CONSTRAINT fk_schedule_plan_blocks_created_by
    FOREIGN KEY (created_by) REFERENCES core.profiles(id) 
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_schedule_plan_blocks_created_by 
    ON appointments.schedule_plan_blocks(created_by);

-- ========================
-- 6. SCHEDULE_BLOCKS → CORE.PROFILES (created_by)
-- ========================

ALTER TABLE appointments.schedule_blocks DROP CONSTRAINT IF EXISTS fk_schedule_blocks_created_by;
ALTER TABLE appointments.schedule_blocks
    ADD CONSTRAINT fk_schedule_blocks_created_by
    FOREIGN KEY (created_by) REFERENCES core.profiles(id) 
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_schedule_blocks_created_by 
    ON appointments.schedule_blocks(created_by);

-- =================================================================
-- V13 COMPLETE: Todas las tablas ahora tienen FKs a core.profiles
-- =================================================================
