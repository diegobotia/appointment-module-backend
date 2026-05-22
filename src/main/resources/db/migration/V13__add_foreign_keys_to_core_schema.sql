-- =================================================================
-- V13: Foreign keys a core (pacientes) y limpieza de doctor_id
-- Idempotente en Supabase: tablas legacy pueden no tener todas las columnas.
-- =================================================================

-- ========================
-- 1. SCHEDULES — quitar FK errónea a core.profiles
-- ========================

ALTER TABLE appointments.schedules DROP CONSTRAINT IF EXISTS fk_schedules_doctor;
ALTER TABLE appointments.schedules DROP CONSTRAINT IF EXISTS schedules_doctor_id_fkey;

CREATE INDEX IF NOT EXISTS idx_schedules_doctor_core
    ON appointments.schedules(doctor_id);

-- ========================
-- 2. APPOINTMENTS → CORE (paciente) + facilities (si aplica)
-- ========================

DO $v13$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'appointments'
          AND column_name = 'facility_id'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'appointments' AND table_name = 'facilities'
    ) THEN
        ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_facility;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_appointments_facility'
              AND conrelid = 'appointments.appointments'::regclass
        ) THEN
            ALTER TABLE appointments.appointments
                ADD CONSTRAINT fk_appointments_facility
                FOREIGN KEY (facility_id) REFERENCES appointments.facilities(id)
                ON DELETE RESTRICT;
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'appointments'
          AND column_name = 'patient_id'
    ) THEN
        ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_patient;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_appointments_patient'
              AND conrelid = 'appointments.appointments'::regclass
        ) THEN
            ALTER TABLE appointments.appointments
                ADD CONSTRAINT fk_appointments_patient
                FOREIGN KEY (patient_id) REFERENCES core.pacientes(id)
                ON DELETE RESTRICT ON UPDATE CASCADE;
        END IF;
    END IF;
END
$v13$;

ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_doctor;
ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS appointments_doctor_id_fkey;

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
-- 3. APPOINTMENT_PARTICIPANTS — sin FK a profiles
-- ========================

ALTER TABLE appointments.appointment_participants DROP CONSTRAINT IF EXISTS fk_appointment_participants_doctor;
ALTER TABLE appointments.appointment_participants DROP CONSTRAINT IF EXISTS appointment_participants_doctor_id_fkey;

CREATE INDEX IF NOT EXISTS idx_appointment_participants_doctor_core
    ON appointments.appointment_participants(doctor_id);

-- ========================
-- 4. SCHEDULE_PLANS — sin FK a profiles
-- ========================

ALTER TABLE appointments.schedule_plans DROP CONSTRAINT IF EXISTS fk_schedule_plans_specialist;
ALTER TABLE appointments.schedule_plans DROP CONSTRAINT IF EXISTS schedule_plans_specialist_id_fkey;

CREATE INDEX IF NOT EXISTS idx_schedule_plans_specialist_core
    ON appointments.schedule_plans(specialist_id);

-- ========================
-- 5–6. created_by → core.profiles (solo si la columna existe)
-- ========================

ALTER TABLE appointments.schedule_plan_blocks
    ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE appointments.schedule_blocks
    ADD COLUMN IF NOT EXISTS created_by UUID;

DO $v13_created_by$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'schedule_plan_blocks'
          AND column_name = 'created_by'
    ) THEN
        ALTER TABLE appointments.schedule_plan_blocks
            DROP CONSTRAINT IF EXISTS fk_schedule_plan_blocks_created_by;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_schedule_plan_blocks_created_by'
              AND conrelid = 'appointments.schedule_plan_blocks'::regclass
        ) THEN
            ALTER TABLE appointments.schedule_plan_blocks
                ADD CONSTRAINT fk_schedule_plan_blocks_created_by
                FOREIGN KEY (created_by) REFERENCES core.profiles(id)
                ON DELETE SET NULL;
        END IF;
        CREATE INDEX IF NOT EXISTS idx_schedule_plan_blocks_created_by
            ON appointments.schedule_plan_blocks(created_by);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'appointments'
          AND table_name = 'schedule_blocks'
          AND column_name = 'created_by'
    ) THEN
        ALTER TABLE appointments.schedule_blocks
            DROP CONSTRAINT IF EXISTS fk_schedule_blocks_created_by;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_schedule_blocks_created_by'
              AND conrelid = 'appointments.schedule_blocks'::regclass
        ) THEN
            ALTER TABLE appointments.schedule_blocks
                ADD CONSTRAINT fk_schedule_blocks_created_by
                FOREIGN KEY (created_by) REFERENCES core.profiles(id)
                ON DELETE SET NULL;
        END IF;
        CREATE INDEX IF NOT EXISTS idx_schedule_blocks_created_by
            ON appointments.schedule_blocks(created_by);
    END IF;
END
$v13_created_by$;

-- =================================================================
-- V13 COMPLETE
-- =================================================================
