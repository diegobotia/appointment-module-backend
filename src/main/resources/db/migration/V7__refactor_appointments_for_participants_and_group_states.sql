-- =============================================
-- V7: PARTICIPANTES DE CITA + ESTADOS/REGLAS EP4
-- =============================================

ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_appointment_type_check;
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_appointment_type_check
        CHECK (appointment_type IN (
            'PRESENCIAL',
            'TELEMEDICINA',
            'JUNTA_MEDICA',
            'TERAPIA_FISICA',
            'TERAPIA_OCUPACIONAL'
        ));

ALTER TABLE appointments
    ADD CONSTRAINT appointments_status_check
        CHECK (status IN (
            'SCHEDULED',
            'PENDIENTE_CONFIRMACION_GRUPO',
            'CONFIRMED',
            'CHECKED_IN',
            'COMPLETED',
            'CANCELLED',
            'NO_SHOW'
        ));

CREATE TABLE appointment_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL,
    participant_order INTEGER NOT NULL CHECK (participant_order BETWEEN 1 AND 2),
    participant_role VARCHAR(20) NOT NULL CHECK (participant_role IN ('PRIMARY', 'SECONDARY')),
    CONSTRAINT uq_appointment_participants_order UNIQUE (appointment_id, participant_order),
    CONSTRAINT uq_appointment_participants_doctor UNIQUE (appointment_id, doctor_id)
);

INSERT INTO appointment_participants (appointment_id, doctor_id, participant_order, participant_role)
SELECT id, doctor_id, 1, 'PRIMARY'
FROM appointments;

CREATE INDEX idx_appointment_participants_doctor_date
    ON appointment_participants(doctor_id, appointment_id);
