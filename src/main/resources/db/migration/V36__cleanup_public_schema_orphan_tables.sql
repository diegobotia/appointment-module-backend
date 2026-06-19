-- =================================================================
-- V36: Limpiar schema public de tablas sobrantes del módulo de citas
-- Fecha: 2026-06-16
-- Descripción: Migraciones V1-V11 crearon tablas en public sin schema
-- prefix. V12+ migró todo a appointments. schema y V14 eliminó algunas
-- (users, roles, specialists, etc.), pero quedaron huérfanas en public:
--   - Tablas funcionales duplicadas en appointments.
--   - Vistas legacy V2/V15/V33
--   - Función update_updated_at_column() (V1, reemplazada en V12)
--   - flyway_schema_history (solo usado por perfil default; en Supabase
--     se usa el perfil supabase con appointments_schema_history)
--   - v_schedule_plan_status se recrea en appointments. schema
--
-- POST-CONDICIÓN:
--   - public solo contiene: appointments_schema_history (Flyway supabase)
--   - appointments. contiene todas las tablas/vistas funcionales de citas
-- =================================================================

-- ========================
-- 1. DROP VIEWS (sin dependencias de tablas)
-- ========================

DROP VIEW IF EXISTS v_schedule_plan_status CASCADE;
DROP VIEW IF EXISTS v_appointments_enriched CASCADE;
DROP VIEW IF EXISTS v_quarterly_plan_status CASCADE;

-- ========================
-- 2. DROP TABLES FUNCIONALES (con CASCADE por FKs internas)
-- ========================

DROP TABLE IF EXISTS appointment_participants CASCADE;
DROP TABLE IF EXISTS schedule_blocks CASCADE;
DROP TABLE IF EXISTS appointments CASCADE;
DROP TABLE IF EXISTS schedule_plan_slots CASCADE;
DROP TABLE IF EXISTS schedule_plan_blocks CASCADE;
DROP TABLE IF EXISTS schedule_plans CASCADE;
DROP TABLE IF EXISTS schedules CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS domain_events CASCADE;
DROP TABLE IF EXISTS facilities CASCADE;

-- ========================
-- 3. DROP FUNCIÓN LEGACY (reemplazada por appointments.update_updated_at_column en V12)
-- ========================

DROP FUNCTION IF EXISTS update_updated_at_column CASCADE;

-- ========================
-- 4. DROP FLYWAY TRACKING DEL PERFIL DEFAULT (no usado en Supabase)
-- ========================

DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- ========================
-- 5. RECREAR v_schedule_plan_status EN appointments. SCHEMA
-- (antes estaba en public porque V33 no usó schema prefix)
-- ========================

CREATE OR REPLACE VIEW appointments.v_schedule_plan_status AS
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

-- =================================================================
-- V36 COMPLETE: public limpio, vistas funcionales en appointments.
-- =================================================================
