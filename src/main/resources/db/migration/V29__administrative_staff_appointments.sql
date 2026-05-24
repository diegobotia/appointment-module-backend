-- =====================================================================
-- V29: Citas administrativas (staff) sin paciente
-- =====================================================================

ALTER TABLE appointments.appointments
    ALTER COLUMN patient_id DROP NOT NULL;

ALTER TABLE appointments.appointments
    DROP CONSTRAINT IF EXISTS appointments_appointment_type_check;

ALTER TABLE appointments.appointments
    ADD CONSTRAINT appointments_appointment_type_check
        CHECK (appointment_type IN (
            'PRESENCIAL',
            'JUNTA_MEDICA',
            'TERAPIA_FISICA',
            'TERAPIA_OCUPACIONAL',
            'STAFF'
        ));

INSERT INTO appointments.facility_resources (sede_id, resource_type, code, display_name, capacity_units, is_active)
VALUES
    (1, 'REUNION_STAFF', 'CONQ-STAFF-01', 'Sala reunión staff 1', 1, true),
    (2, 'REUNION_STAFF', 'BEL-STAFF-01', 'Sala reunión staff 1', 1, true)
ON CONFLICT (sede_id, code) DO NOTHING;
