-- =================================================================
-- V35: Agregar tipo de cita BLOQUEO
-- Fecha: 2026-06-11
-- Descripción: Agrega BLOQUEO al CHECK constraint de appointment_type
--              para permitir citas de médico del dolor sin recurso físico.
-- =================================================================

ALTER TABLE appointments.appointments
  DROP CONSTRAINT IF EXISTS appointments_appointment_type_check;

ALTER TABLE appointments.appointments
  ADD CONSTRAINT appointments_appointment_type_check
  CHECK (appointment_type IN (
    'PRESENCIAL',
    'JUNTA_MEDICA',
    'TERAPIA_FISICA',
    'TERAPIA_OCUPACIONAL',
    'STAFF',
    'BLOQUEO'
  ));
