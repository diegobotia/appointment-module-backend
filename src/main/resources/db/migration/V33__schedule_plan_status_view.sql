-- V33: Reemplazar vista trimestral obsoleta por estado de planes por rango de fechas

DROP VIEW IF EXISTS v_quarterly_plan_status CASCADE;

CREATE OR REPLACE VIEW v_schedule_plan_status AS
SELECT
    sp.id AS plan_id,
    sp.specialist_id,
    TRIM(med.nombre || ' ' || med.apellido) AS specialist_name,
    med.registro AS specialist_registro,
    sp.sede_id,
    sp.start_date,
    sp.end_date,
    sp.version_number,
    sp.is_published,
    sp.is_active_version,
    COUNT(DISTINCT sps.id) AS total_slots,
    COUNT(DISTINCT spb.id) AS total_blocks,
    sp.published_at,
    sp.created_at,
    sp.updated_at
FROM appointments.schedule_plans sp
LEFT JOIN hc.medicos med ON med.id::text = sp.specialist_id::text
LEFT JOIN appointments.schedule_plan_slots sps ON sp.id = sps.schedule_plan_id
LEFT JOIN appointments.schedule_plan_blocks spb ON sp.id = spb.schedule_plan_id
GROUP BY sp.id, med.id, med.nombre, med.apellido, med.registro
ORDER BY sp.start_date DESC, sp.end_date DESC, sp.is_active_version DESC, sp.version_number DESC;
