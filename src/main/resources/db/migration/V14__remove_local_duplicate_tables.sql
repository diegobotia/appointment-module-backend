-- =================================================================
-- V14: Eliminar tablas locales (users, roles, specialists, specialties)
-- Fecha: 2026-05-16
-- Descripción: Remover duplicaciones incompatibles con Supabase Auth
--
-- PRECONDICIONES:
-- - V13 debe haber ejecutado (refactoriza FKs a core.profiles)
-- - Los datos relevantes de users deben estar migrados a core.profiles
-- - Los datos relevantes de specialists deben estar en core.profiles
--
-- TABLA DE ELIMINACIÓN (orden de dependencias):
-- 1. user_facilities     (depende de users y facilities)
-- 2. specialist_specialties (depende de specialists y specialties)
-- 3. user_roles          (depende de users y roles)
-- 4. specialties         (tabla standalone)
-- 5. specialists         (tabla standalone)
-- 6. users               (tabla standalone)
-- 7. roles               (tabla standalone)
--
-- REEMPLAZOS:
-- - users → core.profiles (vinculado a auth.users via Supabase)
-- - roles → core.roles
-- - user_roles → core.profiles.role_id (relación directa)
-- - specialists → core.profiles con rol='Medico' 
-- - specialties → (temporal: specialist_metadata JSONB)
-- - specialist_specialties → specialist_metadata.specialties_json
-- - user_facilities → (futuro: core.facility_authorization)
-- =================================================================

-- ========================
-- Paso 1: Remover FKs a tablas que se van a eliminar
-- ========================

ALTER TABLE IF EXISTS user_facilities DROP CONSTRAINT IF EXISTS fk_user_facilities_user;
ALTER TABLE IF EXISTS user_facilities DROP CONSTRAINT IF EXISTS fk_user_facilities_facility;

ALTER TABLE IF EXISTS specialist_specialties DROP CONSTRAINT IF EXISTS specialist_specialties_specialist_id_fkey;
ALTER TABLE IF EXISTS specialist_specialties DROP CONSTRAINT IF EXISTS specialist_specialties_specialty_id_fkey;

ALTER TABLE IF EXISTS user_roles DROP CONSTRAINT IF EXISTS user_roles_user_id_fkey;
ALTER TABLE IF EXISTS user_roles DROP CONSTRAINT IF EXISTS user_roles_role_id_fkey;

-- ========================
-- Paso 2: Eliminar tablas (en orden inverso de dependencias)
-- ========================

DROP TABLE IF EXISTS user_facilities CASCADE;
DROP TABLE IF EXISTS specialist_specialties CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS specialties CASCADE;
DROP TABLE IF EXISTS specialists CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- ========================
-- Paso 3: Auditoría y documentación
-- ========================

-- Las siguientes tablas se han eliminado por duplicación con core schema:
-- 
-- users:
--   Razón: core.profiles está vinculado a Supabase auth.users
--   Migración: Cualquier usuario local debe estar en core.profiles
--   Referencia: MIGRATION_CONTRACTS.md (congelado)
--
-- roles:
--   Razón: core.roles es la fuente única de verdad
--   Migración: Cualquier rol local debe estar en core.roles
--   Referencia: core.roles.nombre = 'Medico', 'Enfermera', 'Admin', etc.
--
-- user_roles:
--   Razón: core.profiles.role_id reemplaza esta relación M:M
--   Migración: Cada usuario ahora tiene exactamente UN rol en profile.role_id
--   Nota: Si un usuario necesita múltiples roles, usar feature flag o tabla auxiliar
--
-- specialists:
--   Razón: core.profiles con rol_id apuntando a rol 'Medico'
--   Migración: Los médicos están en core.profiles
--   Referencia: DoctorApplicationService.findAvailableDoctors() busca por core.profiles
--
-- specialties:
--   Razón: Especialidades van en hc.specialties (bloqueado) o specialist_metadata (temporal)
--   Migración: Usar specialist_metadata.specialties_json JSONB
--   Referencia: V12__baseline_supabase_migration_appointments_schema.sql línea 117+
--
-- specialist_specialties:
--   Razón: specialist_metadata.specialties_json JSONB reemplaza esta relación M:M
--   Migración: {"primary": "CARDIOLOGIA", "secondary": ["EMERGENCIAS"]}
--   Referencia: DoctorApplicationService.findAvailableDoctors() parsea JSON

-- ========================
-- Paso 4: Validación post-eliminación
-- ========================

-- Ejecutar después de esta migración:
-- SELECT COUNT(*) FROM information_schema.tables 
-- WHERE table_schema = 'public' AND table_name IN 
--   ('users', 'roles', 'specialists', 'specialties', 'user_roles', 
--    'specialist_specialties', 'user_facilities')
-- Debería retornar: 0

-- =================================================================
-- V14 COMPLETE: Tablas locales eliminadas, solo core schema subsiste
-- =================================================================
