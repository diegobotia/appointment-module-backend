-- =================================================================
-- V17: ACTUALIZAR REFERENCIAS DE MEDICOS A HC.MEDICO
-- Fecha: 18 de mayo de 2026
-- Descripción: Cambia las referencias de appointments.* hacia hc.medico
--              en lugar de tablas locales o core.profiles.
--              Establece FK correctas desde:
--              - schedules.doctor_id → hc.medico.id
--              - appointments.doctor_id → hc.medico.id
--              - appointments.secondary_doctor_id → hc.medico.id
--              - appointment_participants.doctor_id → hc.medico.id
--              - schedule_plans.specialist_id → hc.medico.id
-- =================================================================

-- ============================================================
-- 1. REMOVER CONSTRAINT EXISTENTES A core.profiles (V13)
-- ============================================================

-- Remover FKs que apuntan a core.profiles
ALTER TABLE IF EXISTS appointments.schedules DROP CONSTRAINT IF EXISTS schedules_doctor_id_fkey;
ALTER TABLE IF EXISTS appointments.appointments DROP CONSTRAINT IF EXISTS appointments_doctor_id_fkey;
ALTER TABLE IF EXISTS appointments.appointments DROP CONSTRAINT IF EXISTS appointments_secondary_doctor_id_fkey;
ALTER TABLE IF EXISTS appointments.appointment_participants DROP CONSTRAINT IF EXISTS appointment_participants_doctor_id_fkey;
ALTER TABLE IF EXISTS appointments.schedule_plans DROP CONSTRAINT IF EXISTS schedule_plans_specialist_id_fkey;

-- ============================================================
-- 2. AGREGAR NUEVAS CONSTRAINT A HC.MEDICO
-- ============================================================

ALTER TABLE appointments.schedules
    ADD CONSTRAINT schedules_doctor_id_fkey
    FOREIGN KEY (doctor_id) REFERENCES hc.medico(id) ON DELETE RESTRICT;

ALTER TABLE appointments.appointments
    ADD CONSTRAINT appointments_doctor_id_fkey
    FOREIGN KEY (doctor_id) REFERENCES hc.medico(id) ON DELETE RESTRICT;

-- Solo agregar si la columna existe
ALTER TABLE appointments.appointments
    ADD CONSTRAINT appointments_secondary_doctor_id_fkey
    FOREIGN KEY (secondary_doctor_id) REFERENCES hc.medico(id) ON DELETE RESTRICT;

ALTER TABLE appointments.appointment_participants
    ADD CONSTRAINT appointment_participants_doctor_id_fkey
    FOREIGN KEY (doctor_id) REFERENCES hc.medico(id) ON DELETE RESTRICT;

ALTER TABLE appointments.schedule_plans
    ADD CONSTRAINT schedule_plans_specialist_id_fkey
    FOREIGN KEY (specialist_id) REFERENCES hc.medico(id) ON DELETE RESTRICT;

-- ============================================================
-- 3. RECREAR ÍNDICES PARA PERFORMANCE
-- ============================================================

-- Recrear índices en schedules
DROP INDEX IF EXISTS idx_schedules_doctor_day;
CREATE INDEX idx_schedules_doctor_day ON appointments.schedules(doctor_id, day_of_week);

DROP INDEX IF EXISTS idx_appointment_participants_doctor;
CREATE INDEX idx_appointment_participants_doctor ON appointments.appointment_participants(doctor_id, appointment_id);

DROP INDEX IF EXISTS idx_schedule_plans_specialist;
CREATE INDEX idx_schedule_plans_specialist ON appointments.schedule_plans(specialist_id, plan_year, plan_quarter);

-- ============================================================
-- 4. COMENTARIOS PARA DOCUMENTACIÓN
-- ============================================================

COMMENT ON CONSTRAINT schedules_doctor_id_fkey ON appointments.schedules 
    IS 'FK a hc.medico: El médico debe existir en el sistema de health center';

COMMENT ON CONSTRAINT appointments_doctor_id_fkey ON appointments.appointments 
    IS 'FK a hc.medico: El médico principal de la cita debe existir en HC';

COMMENT ON CONSTRAINT appointments_secondary_doctor_id_fkey ON appointments.appointments 
    IS 'FK a hc.medico: El médico secundario (si aplica) debe existir en HC';

COMMENT ON CONSTRAINT appointment_participants_doctor_id_fkey ON appointments.appointment_participants 
    IS 'FK a hc.medico: Cada participante médico debe existir en HC';

COMMENT ON CONSTRAINT schedule_plans_specialist_id_fkey ON appointments.schedule_plans 
    IS 'FK a hc.medico: El especialista que planifica debe existir en HC';
