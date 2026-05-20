-- =================================================================
-- V15: Agregar vistas cross-schema para consultas comunes
-- Fecha: 2026-05-16
-- Descripción: Facilita queries entre appointments, core, y futuro hc
--
-- VISTAS CREADAS:
-- 1. v_appointments_complete: Citas con datos de paciente, médico y sede
-- 2. v_doctor_available_slots: Horarios disponibles de médicos
-- 3. v_appointments_by_facility: Citas agrupadas por sede
-- 4. v_doctor_specialties: Especialidades de médicos (TEMPORAL)
-- 5. v_appointment_participants_complete: Participantes con datos enriquecidos
--
-- NOTA: Todas son vistas de LECTURA. Para modificar, usar DML sobre tablas base.
-- =================================================================

-- ========================
-- V1: APPOINTMENTS COMPLETE
-- ========================
-- Citas con información enriquecida: paciente, médico, sede

DROP VIEW IF EXISTS v_appointments_enriched CASCADE;

CREATE OR REPLACE VIEW v_appointments_complete AS
SELECT 
    a.id,
    a.patient_id,
    COALESCE(p.nombres || ' ' || p.apellidos, 'Sin nombre') AS patient_name,
    p.num_identificacion AS patient_id_number,
    a.doctor_id,
    COALESCE(prof.name, 'Sin asignar') AS doctor_name,
    prof.email AS doctor_email,
    a.facility_id,
    f.name AS facility_name,
    f.code AS facility_code,
    a.appointment_date,
    a.appointment_time,
    a.duration_minutes,
    a.appointment_type,
    a.status,
    a.reason,
    a.created_at,
    a.updated_at,
    -- Calcula si es inminente (próximas 30 minutos)
    CASE 
        WHEN a.status IN ('SCHEDULED', 'CONFIRMED') 
            AND a.appointment_date = CURRENT_DATE 
            AND a.appointment_time BETWEEN CURRENT_TIME AND (CURRENT_TIME + INTERVAL '30 minutes')
        THEN true
        ELSE false
    END AS is_upcoming_soon,
    -- Calcula si ya pasó
    CASE 
        WHEN a.appointment_date < CURRENT_DATE 
            OR (a.appointment_date = CURRENT_DATE AND a.appointment_time < CURRENT_TIME)
        THEN true
        ELSE false
    END AS is_past
FROM appointments a
LEFT JOIN core.pacientes p ON a.patient_id = p.id
LEFT JOIN core.profiles prof ON a.doctor_id = prof.id
LEFT JOIN appointments.facilities f ON a.facility_id = f.id;

-- ========================
-- V2: DOCTOR AVAILABLE SLOTS
-- ========================
-- Horarios disponibles de cada médico con conteo de citas

CREATE OR REPLACE VIEW v_doctor_available_slots AS
SELECT 
    s.id as schedule_id,
    s.doctor_id,
    prof.name as doctor_name,
    prof.email,
    s.facility_id,
    f.name as facility_name,
    f.code as facility_code,
    s.day_of_week,
    s.start_time,
    s.end_time,
    s.slot_duration_minutes,
    s.max_patients_per_slot,
    s.is_active as schedule_active,
    COALESCE(COUNT(DISTINCT a.id), 0) as appointments_count,
    -- Calcula si el horario está lleno
    CASE 
        WHEN COALESCE(COUNT(DISTINCT a.id), 0) >= s.max_patients_per_slot
        THEN true
        ELSE false
    END AS is_full,
    -- Calcula capacidad disponible
    (s.max_patients_per_slot - COALESCE(COUNT(DISTINCT a.id), 0)) as available_slots
FROM appointments.schedules s
JOIN core.profiles prof ON s.doctor_id = prof.id
JOIN appointments.facilities f ON s.facility_id = f.id
LEFT JOIN appointments.appointments a ON s.id = a.schedule_id 
    AND a.status IN ('SCHEDULED', 'CONFIRMED')
WHERE s.is_active = true
GROUP BY s.id, prof.id, f.id;

-- ========================
-- V3: APPOINTMENTS BY FACILITY
-- ========================
-- Citas agrupadas por sede con métricas

CREATE OR REPLACE VIEW v_appointments_by_facility AS
SELECT 
    f.id,
    f.name as facility_name,
    f.code,
    f.is_active,
    CURRENT_DATE as report_date,
    a.appointment_date,
    a.status,
    COUNT(*) as appointment_count,
    COUNT(DISTINCT a.patient_id) as unique_patients,
    COUNT(DISTINCT a.doctor_id) as doctors_involved,
    ROUND(AVG(a.duration_minutes)::numeric, 2) as avg_duration_minutes,
    MIN(a.appointment_time) as earliest_appointment,
    MAX(a.appointment_time) as latest_appointment
FROM appointments.facilities f
LEFT JOIN appointments.appointments a ON f.id = a.facility_id
GROUP BY f.id, a.appointment_date, a.status
ORDER BY f.id, a.appointment_date DESC;

-- ========================
-- V4: DOCTOR SPECIALTIES (TEMPORAL)
-- ========================
-- Especialidades de médicos (de specialist_metadata JSON hasta hc.specialties)

CREATE OR REPLACE VIEW v_doctor_specialties AS
SELECT 
    prof.id as doctor_id,
    prof.name,
    prof.email,
    COALESCE(sm.specialties_json ->> 'primary', 'Sin especialidad') as primary_specialty,
    CASE 
        WHEN sm.specialties_json -> 'secondary' IS NOT NULL 
        THEN ARRAY(SELECT jsonb_array_elements_text(sm.specialties_json -> 'secondary'))
        ELSE ARRAY[]::TEXT[]
    END as secondary_specialties,
    sm.max_patients_per_slot,
    sm.is_active as specialties_active,
    COALESCE(sm.synced_from_hc, false) as synced_from_hc,
    sm.created_at,
    sm.updated_at
FROM core.profiles prof
LEFT JOIN appointments.specialist_metadata sm ON prof.id = sm.profile_id
WHERE prof.role_id IN (
    SELECT id FROM core.roles WHERE nombre = 'Medico'
)
ORDER BY prof.name;

-- ========================
-- V5: APPOINTMENT PARTICIPANTS (ENRIQUECIDA)
-- ========================
-- Participantes de citas con datos de médicos

CREATE OR REPLACE VIEW v_appointment_participants_complete AS
SELECT 
    ap.id,
    ap.appointment_id,
    a.patient_id,
    COALESCE(p.nombres || ' ' || p.apellidos, 'Sin nombre') AS patient_name,
    a.appointment_date,
    a.appointment_time,
    a.facility_id,
    f.name as facility_name,
    ap.doctor_id,
    COALESCE(prof.name, 'Sin asignar') AS doctor_name,
    prof.email AS doctor_email,
    ap.participant_order,
    ap.participant_role,
    a.status as appointment_status
FROM appointments.appointment_participants ap
JOIN appointments.appointments a ON ap.appointment_id = a.id
LEFT JOIN core.pacientes p ON a.patient_id = p.id
LEFT JOIN core.profiles prof ON ap.doctor_id = prof.id
LEFT JOIN appointments.facilities f ON a.facility_id = f.id
ORDER BY ap.appointment_id, ap.participant_order;

-- ========================
-- V6: APPOINTMENT CONFIRMATIONS PENDING
-- ========================
-- Citas pendientes de confirmación de grupo (junta médica)

CREATE OR REPLACE VIEW v_appointments_pending_group_confirmation AS
SELECT 
    a.id,
    a.patient_id,
    COALESCE(p.nombres || ' ' || p.apellidos, 'Sin nombre') AS patient_name,
    a.appointment_date,
    a.appointment_time,
    f.name as facility_name,
    a.appointment_type,
    a.status,
    COUNT(DISTINCT ap.id) as participant_count,
    STRING_AGG(DISTINCT prof.name, ', ') AS participants_names
FROM appointments.appointments a
LEFT JOIN core.pacientes p ON a.patient_id = p.id
LEFT JOIN appointments.facilities f ON a.facility_id = f.id
LEFT JOIN appointments.appointment_participants ap ON a.id = ap.appointment_id
LEFT JOIN core.profiles prof ON ap.doctor_id = prof.id
WHERE a.status = 'PENDIENTE_CONFIRMACION_GRUPO'
GROUP BY a.id, p.id, f.id
ORDER BY a.appointment_date;

-- ========================
-- V7: SCHEDULES SUMMARY BY DOCTOR
-- ========================
-- Resumen de agendas por médico

CREATE OR REPLACE VIEW v_schedules_summary_by_doctor AS
SELECT 
    prof.id as doctor_id,
    prof.name as doctor_name,
    prof.email,
    f.id as facility_id,
    f.name as facility_name,
    COUNT(DISTINCT s.id) as total_schedules,
    STRING_AGG(DISTINCT 'Día ' || s.day_of_week, ', ') AS days_scheduled,
    MIN(s.start_time) as earliest_start,
    MAX(s.end_time) as latest_end,
    COUNT(CASE WHEN s.is_active THEN 1 END) as active_schedules
FROM core.profiles prof
LEFT JOIN appointments.schedules s ON prof.id = s.doctor_id
LEFT JOIN appointments.facilities f ON s.facility_id = f.id
WHERE prof.role_id IN (
    SELECT id FROM core.roles WHERE nombre = 'Medico'
)
GROUP BY prof.id, f.id
ORDER BY prof.name, f.name;

-- ========================
-- V8: QUARTERLY PLAN STATUS
-- ========================
-- Estado de planes trimestrales

CREATE OR REPLACE VIEW v_quarterly_plan_status AS
SELECT 
    sp.specialist_id,
    prof.name as specialist_name,
    prof.email,
    sp.plan_year,
    sp.plan_quarter,
    sp.version_number,
    sp.is_published,
    sp.is_active_version,
    COUNT(DISTINCT sps.id) as total_slots,
    COUNT(DISTINCT spb.id) as total_blocks,
    sp.published_at,
    sp.created_at,
    sp.updated_at
FROM appointments.schedule_plans sp
JOIN core.profiles prof ON sp.specialist_id = prof.id
LEFT JOIN appointments.schedule_plan_slots sps ON sp.id = sps.schedule_plan_id
LEFT JOIN appointments.schedule_plan_blocks spb ON sp.id = spb.schedule_plan_id
GROUP BY sp.id, prof.id
ORDER BY sp.plan_year DESC, sp.plan_quarter DESC, sp.is_active_version DESC;

-- =================================================================
-- V15 COMPLETE: 8 vistas cross-schema creadas exitosamente
-- =================================================================
-- 
-- Para usar estas vistas:
-- SELECT * FROM v_appointments_complete WHERE appointment_date = CURRENT_DATE;
-- SELECT * FROM v_doctor_available_slots WHERE doctor_id = '...'::uuid;
-- SELECT * FROM v_appointments_by_facility WHERE facility_id = '...'::uuid;
-- SELECT * FROM v_doctor_specialties WHERE doctor_active = true;
--
-- Para documentación completa, ver: docs/ITERATION_2_MIGRATION_PLAN_V13_V15.md
