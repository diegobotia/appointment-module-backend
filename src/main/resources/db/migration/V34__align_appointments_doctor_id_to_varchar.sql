-- =============================================
-- V34: Align appointments.doctor_id to VARCHAR(64)
-- =============================================
-- V12 baseline defined doctor_id as VARCHAR(64) to match
-- hc.medicos.id, but the actual column remained UUID from V2.
-- This migration aligns it with schedules.doctor_id and
-- appointment_participants.doctor_id which are already VARCHAR.
-- =============================================

-- Drop FK constraints that reference doctor_id (UUID) before altering type.
-- V12 intentionally removed these FKs (doctor_id is a logical reference to hc.medicos.id),
-- but they may still exist in the database.
ALTER TABLE IF EXISTS appointments.appointments
    DROP CONSTRAINT IF EXISTS fk_appointments_doctor,
    DROP CONSTRAINT IF EXISTS appointments_doctor_id_fkey;

ALTER TABLE appointments.appointments
    ALTER COLUMN doctor_id TYPE VARCHAR(64) USING doctor_id::text;
