-- =============================================
-- V9: CITAS CON SEDE ASOCIADA
-- =============================================

ALTER TABLE appointments
    ADD COLUMN facility_id UUID;

UPDATE appointments a
SET facility_id = s.facility_id
FROM schedules s
WHERE a.schedule_id = s.id
  AND a.facility_id IS NULL;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_facility
        FOREIGN KEY (facility_id) REFERENCES facilities(id);

ALTER TABLE appointments
    ALTER COLUMN facility_id SET NOT NULL;

CREATE INDEX idx_appointments_facility_date_status ON appointments(facility_id, appointment_date, status);

DROP VIEW IF EXISTS v_appointments_enriched;

CREATE VIEW v_appointments_enriched AS
SELECT 
    a.id,
    a.patient_id,
    a.doctor_id,
    a.facility_id,
    a.appointment_date,
    a.appointment_time,
    a.duration_minutes,
    a.appointment_type,
    a.status,
    a.reason,
    a.created_at,
    s.specialty,
    CASE 
        WHEN a.status IN ('SCHEDULED', 'CONFIRMED') 
            AND a.appointment_date = CURRENT_DATE 
            AND a.appointment_time <= CURRENT_TIME + INTERVAL '30 minutes'
        THEN true
        ELSE false
    END AS is_upcoming_soon
FROM appointments a
LEFT JOIN schedules s ON a.schedule_id = s.id;