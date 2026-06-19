-- =====================================================================
-- V27: Reemplazar appointments.facilities por core.sede
-- Mapeo acordado (prod Supabase):
--   sede_id 1 → Conquistadores (CENTIR DEL SUR S.A.S.)
--   sede_id 2 → Belén (CENTRO INTEGRAL DE REHABILITACIÓN)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. Bootstrap local de core.sede (no-op si ya existe en Supabase)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS core.sede (
    id INTEGER PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    direccion UUID,
    matricula_mercantil VARCHAR(50)
);

INSERT INTO core.sede (id, nombre, direccion, matricula_mercantil)
VALUES
    (1, 'CENTIR DEL SUR S.A.S.', gen_random_uuid(), '2151038302'),
    (2, 'CENTRO INTEGRAL DE REHABILITACION', gen_random_uuid(), '2174165402')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 1. Aliases de códigos (n8n y legacy)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments.sede_code_aliases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id INTEGER NOT NULL,
    alias_code VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sede_code_aliases_code UNIQUE (alias_code),
    CONSTRAINT fk_sede_code_aliases_sede
        FOREIGN KEY (sede_id) REFERENCES core.sede(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sede_code_aliases_sede
    ON appointments.sede_code_aliases(sede_id);

INSERT INTO appointments.sede_code_aliases (sede_id, alias_code)
VALUES
    (1, 'CONQUISTADORES'),
    (1, 'SEDE_CONQUISTADORES'),
    (1, 'SEDE_PRINCIPAL'),
    (2, 'BELEN'),
    (2, 'SEDE_BELEN'),
    (2, 'SEDE_NORTE')
ON CONFLICT (alias_code) DO NOTHING;

-- Migrar aliases existentes desde facilities (si aplica)
INSERT INTO appointments.sede_code_aliases (sede_id, alias_code)
SELECT
    CASE
        WHEN f.code IN ('SEDE_CONQUISTADORES', 'SEDE_PRINCIPAL') THEN 1
        WHEN f.code IN ('SEDE_BELEN', 'SEDE_NORTE') THEN 2
        ELSE NULL
    END,
    a.alias_code
FROM appointments.facility_code_aliases a
JOIN appointments.facilities f ON f.id = a.facility_id
WHERE CASE
        WHEN f.code IN ('SEDE_CONQUISTADORES', 'SEDE_PRINCIPAL') THEN 1
        WHEN f.code IN ('SEDE_BELEN', 'SEDE_NORTE') THEN 2
        ELSE NULL
    END IS NOT NULL
ON CONFLICT (alias_code) DO NOTHING;

-- ---------------------------------------------------------------------
-- 1b. Eliminar vistas que dependen de facility_id (V12/V15) antes del DROP COLUMN
-- ---------------------------------------------------------------------
DROP VIEW IF EXISTS appointments.v_appointments_complete CASCADE;
DROP VIEW IF EXISTS appointments.v_doctor_available_slots CASCADE;
DROP VIEW IF EXISTS appointments.v_appointments_by_facility CASCADE;
DROP VIEW IF EXISTS appointments.v_appointments_by_sede CASCADE;

DROP VIEW IF EXISTS v_appointments_complete CASCADE;
DROP VIEW IF EXISTS v_doctor_available_slots CASCADE;
DROP VIEW IF EXISTS v_appointments_by_facility CASCADE;
DROP VIEW IF EXISTS v_appointment_participants_complete CASCADE;
DROP VIEW IF EXISTS v_appointments_pending_group_confirmation CASCADE;
DROP VIEW IF EXISTS v_schedules_summary_by_doctor CASCADE;
DROP VIEW IF EXISTS v_quarterly_plan_status CASCADE;
DROP VIEW IF EXISTS v_doctor_specialties CASCADE;

-- ---------------------------------------------------------------------
-- 2. appointments: facility_id → sede_id
-- ---------------------------------------------------------------------
ALTER TABLE appointments.appointments ADD COLUMN IF NOT EXISTS sede_id INTEGER;

UPDATE appointments.appointments a
SET sede_id = CASE
    WHEN f.code IN ('SEDE_CONQUISTADORES', 'SEDE_PRINCIPAL') THEN 1
    WHEN f.code IN ('SEDE_BELEN', 'SEDE_NORTE') THEN 2
    ELSE a.sede_id
END
FROM appointments.facilities f
WHERE f.id = a.facility_id AND a.sede_id IS NULL;

ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_facility;
ALTER TABLE appointments.appointments DROP CONSTRAINT IF EXISTS fk_appointments_sede;
ALTER TABLE appointments.appointments DROP COLUMN IF EXISTS facility_id;
ALTER TABLE appointments.appointments ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE appointments.appointments
    ADD CONSTRAINT fk_appointments_sede
    FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE RESTRICT;

DROP INDEX IF EXISTS appointments.idx_appointments_facility_date_status;
CREATE INDEX IF NOT EXISTS idx_appointments_sede_date_status
    ON appointments.appointments(sede_id, appointment_date, status);

-- ---------------------------------------------------------------------
-- 3. schedules
-- ---------------------------------------------------------------------
ALTER TABLE appointments.schedules ADD COLUMN IF NOT EXISTS sede_id INTEGER;

UPDATE appointments.schedules s
SET sede_id = CASE
    WHEN f.code IN ('SEDE_CONQUISTADORES', 'SEDE_PRINCIPAL') THEN 1
    WHEN f.code IN ('SEDE_BELEN', 'SEDE_NORTE') THEN 2
    ELSE s.sede_id
END
FROM appointments.facilities f
WHERE f.id = s.facility_id AND s.sede_id IS NULL;

ALTER TABLE appointments.schedules DROP CONSTRAINT IF EXISTS fk_schedules_facility;
ALTER TABLE appointments.schedules DROP CONSTRAINT IF EXISTS fk_schedules_sede;
ALTER TABLE appointments.schedules DROP COLUMN IF EXISTS facility_id;
ALTER TABLE appointments.schedules ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE appointments.schedules
    ADD CONSTRAINT fk_schedules_sede
    FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE RESTRICT ON UPDATE CASCADE;

DROP INDEX IF EXISTS appointments.idx_schedules_facility;
DROP INDEX IF EXISTS appointments.idx_schedules_doctor_facility_day;
CREATE INDEX IF NOT EXISTS idx_schedules_sede ON appointments.schedules(sede_id);
CREATE INDEX IF NOT EXISTS idx_schedules_doctor_sede_day
    ON appointments.schedules(doctor_id, sede_id, day_of_week);

-- ---------------------------------------------------------------------
-- 4. Horario institucional
-- ---------------------------------------------------------------------
ALTER TABLE appointments.facility_operating_hours ADD COLUMN IF NOT EXISTS sede_id INTEGER;

UPDATE appointments.facility_operating_hours h
SET sede_id = CASE
    WHEN f.code IN ('SEDE_CONQUISTADORES', 'SEDE_PRINCIPAL') THEN 1
    WHEN f.code IN ('SEDE_BELEN', 'SEDE_NORTE') THEN 2
    ELSE h.sede_id
END
FROM appointments.facilities f
WHERE f.id = h.facility_id AND h.sede_id IS NULL;

ALTER TABLE appointments.facility_operating_hours DROP CONSTRAINT IF EXISTS fk_facility_operating_hours_facility;
ALTER TABLE appointments.facility_operating_hours DROP CONSTRAINT IF EXISTS uq_facility_operating_hours_facility_day;
ALTER TABLE appointments.facility_operating_hours DROP CONSTRAINT IF EXISTS fk_facility_operating_hours_sede;
ALTER TABLE appointments.facility_operating_hours DROP CONSTRAINT IF EXISTS uq_facility_operating_hours_sede_day;
ALTER TABLE appointments.facility_operating_hours DROP COLUMN IF EXISTS facility_id;
ALTER TABLE appointments.facility_operating_hours ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE appointments.facility_operating_hours
    ADD CONSTRAINT fk_facility_operating_hours_sede
    FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE appointments.facility_operating_hours
    ADD CONSTRAINT uq_facility_operating_hours_sede_day UNIQUE (sede_id, day_of_week);

DROP INDEX IF EXISTS appointments.idx_facility_operating_hours_facility;
CREATE INDEX IF NOT EXISTS idx_facility_operating_hours_sede
    ON appointments.facility_operating_hours(sede_id);

-- ---------------------------------------------------------------------
-- 5. Inventario físico
-- ---------------------------------------------------------------------
ALTER TABLE appointments.facility_resources ADD COLUMN IF NOT EXISTS sede_id INTEGER;

UPDATE appointments.facility_resources r
SET sede_id = CASE
    WHEN f.code IN ('SEDE_CONQUISTADORES', 'SEDE_PRINCIPAL') THEN 1
    WHEN f.code IN ('SEDE_BELEN', 'SEDE_NORTE') THEN 2
    ELSE r.sede_id
END
FROM appointments.facilities f
WHERE f.id = r.facility_id AND r.sede_id IS NULL;

ALTER TABLE appointments.facility_resources DROP CONSTRAINT IF EXISTS fk_facility_resources_facility;
ALTER TABLE appointments.facility_resources DROP CONSTRAINT IF EXISTS uq_facility_resources_facility_code;
ALTER TABLE appointments.facility_resources DROP CONSTRAINT IF EXISTS fk_facility_resources_sede;
ALTER TABLE appointments.facility_resources DROP CONSTRAINT IF EXISTS uq_facility_resources_sede_code;
ALTER TABLE appointments.facility_resources DROP COLUMN IF EXISTS facility_id;
ALTER TABLE appointments.facility_resources ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE appointments.facility_resources
    ADD CONSTRAINT fk_facility_resources_sede
    FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE appointments.facility_resources
    ADD CONSTRAINT uq_facility_resources_sede_code UNIQUE (sede_id, code);

DROP INDEX IF EXISTS appointments.idx_facility_resources_facility_type_active;
CREATE INDEX IF NOT EXISTS idx_facility_resources_sede_type_active
    ON appointments.facility_resources(sede_id, resource_type, is_active);

-- ---------------------------------------------------------------------
-- 6. Allocations de recursos
-- ---------------------------------------------------------------------
ALTER TABLE appointments.appointment_resource_allocations ADD COLUMN IF NOT EXISTS sede_id INTEGER;

UPDATE appointments.appointment_resource_allocations a
SET sede_id = CASE
    WHEN f.code IN ('SEDE_CONQUISTADORES', 'SEDE_PRINCIPAL') THEN 1
    WHEN f.code IN ('SEDE_BELEN', 'SEDE_NORTE') THEN 2
    ELSE a.sede_id
END
FROM appointments.facilities f
WHERE f.id = a.facility_id AND a.sede_id IS NULL;

ALTER TABLE appointments.appointment_resource_allocations DROP CONSTRAINT IF EXISTS fk_appointment_resource_allocations_facility;
ALTER TABLE appointments.appointment_resource_allocations DROP CONSTRAINT IF EXISTS fk_appointment_resource_allocations_sede;
ALTER TABLE appointments.appointment_resource_allocations DROP COLUMN IF EXISTS facility_id;
ALTER TABLE appointments.appointment_resource_allocations ALTER COLUMN sede_id SET NOT NULL;
ALTER TABLE appointments.appointment_resource_allocations
    ADD CONSTRAINT fk_appointment_resource_allocations_sede
    FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE RESTRICT;

DROP INDEX IF EXISTS appointments.idx_appointment_resource_allocations_lookup;
CREATE INDEX IF NOT EXISTS idx_appointment_resource_allocations_lookup
    ON appointments.appointment_resource_allocations(sede_id, resource_type, appointment_date, start_time, end_time);

-- ---------------------------------------------------------------------
-- 7. Eliminar duplicado local
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS appointments.facility_code_aliases CASCADE;
DROP TABLE IF EXISTS appointments.facilities CASCADE;

-- ---------------------------------------------------------------------
-- 8. Vistas cross-schema (sede_id; recreadas tras DROP facility_id)
-- ---------------------------------------------------------------------

CREATE OR REPLACE VIEW appointments.v_appointments_complete AS
SELECT
    a.id,
    a.patient_id,
    COALESCE(p.nombres || ' ' || p.apellidos, 'Sin nombre') AS patient_name,
    p.num_identificacion AS patient_id_number,
    a.doctor_id,
    COALESCE(TRIM(med.nombre || ' ' || med.apellido), 'Sin asignar') AS doctor_name,
    med.registro AS doctor_registro,
    a.sede_id,
    s.nombre AS sede_nombre,
    a.appointment_date,
    a.appointment_time,
    a.duration_minutes,
    a.appointment_type,
    a.status,
    a.reason,
    a.created_at,
    a.updated_at,
    CASE
        WHEN a.status IN ('SCHEDULED', 'CONFIRMED')
            AND a.appointment_date = CURRENT_DATE
            AND a.appointment_time BETWEEN CURRENT_TIME AND (CURRENT_TIME + INTERVAL '30 minutes')
        THEN true
        ELSE false
    END AS is_upcoming_soon,
    CASE
        WHEN a.appointment_date < CURRENT_DATE
            OR (a.appointment_date = CURRENT_DATE AND a.appointment_time < CURRENT_TIME)
        THEN true
        ELSE false
    END AS is_past
FROM appointments.appointments a
LEFT JOIN core.pacientes p ON a.patient_id = p.id
LEFT JOIN hc.medicos med ON med.id::text = a.doctor_id::text
LEFT JOIN core.sede s ON a.sede_id = s.id;

CREATE OR REPLACE VIEW appointments.v_doctor_available_slots AS
SELECT
    s.id AS schedule_id,
    s.doctor_id,
    COALESCE(TRIM(med.nombre || ' ' || med.apellido), 'Sin asignar') AS doctor_name,
    med.registro AS doctor_registro,
    s.sede_id,
    sede.nombre AS sede_nombre,
    s.day_of_week,
    s.start_time,
    s.end_time,
    s.slot_duration_minutes,
    s.max_patients_per_slot,
    s.is_active AS schedule_active,
    COALESCE(COUNT(DISTINCT a.id), 0) AS appointments_count,
    CASE
        WHEN COALESCE(COUNT(DISTINCT a.id), 0) >= s.max_patients_per_slot THEN true
        ELSE false
    END AS is_full,
    (s.max_patients_per_slot - COALESCE(COUNT(DISTINCT a.id), 0)) AS available_slots
FROM appointments.schedules s
LEFT JOIN hc.medicos med ON med.id::text = s.doctor_id::text
JOIN core.sede sede ON s.sede_id = sede.id
LEFT JOIN appointments.appointments a ON s.id = a.schedule_id
    AND a.status IN ('SCHEDULED', 'CONFIRMED')
WHERE s.is_active = true
GROUP BY s.id, med.id, med.nombre, med.apellido, med.registro, sede.id;

CREATE OR REPLACE VIEW appointments.v_appointments_by_sede AS
SELECT
    s.id AS sede_id,
    s.nombre AS sede_nombre,
    CURRENT_DATE AS report_date,
    a.appointment_date,
    a.status,
    COUNT(*) AS appointment_count,
    COUNT(DISTINCT a.patient_id) AS unique_patients,
    COUNT(DISTINCT a.doctor_id) AS doctors_involved,
    ROUND(AVG(a.duration_minutes)::numeric, 2) AS avg_duration_minutes,
    MIN(a.appointment_time) AS earliest_appointment,
    MAX(a.appointment_time) AS latest_appointment
FROM core.sede s
LEFT JOIN appointments.appointments a ON s.id = a.sede_id
    AND a.appointment_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY s.id, s.nombre, a.appointment_date, a.status;
