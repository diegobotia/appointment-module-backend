-- =============================================
-- V11: REMOVE TELEMEDICINA APPOINTMENT TYPE
-- =============================================

-- Normalize legacy rows before tightening constraint.
UPDATE appointments
SET appointment_type = 'PRESENCIAL'
WHERE appointment_type = 'TELEMEDICINA';

ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_appointment_type_check;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_appointment_type_check
        CHECK (appointment_type IN (
            'PRESENCIAL',
            'JUNTA_MEDICA',
            'TERAPIA_FISICA',
            'TERAPIA_OCUPACIONAL'
        ));
