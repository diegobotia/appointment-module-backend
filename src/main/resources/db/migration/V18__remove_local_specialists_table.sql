-- =================================================================
-- V18: ELIMINAR TABLA LOCAL SPECIALISTS
-- Fecha: 18 de mayo de 2026
-- Descripción: Elimina la tabla local appointments.specialists
--              ya que los médicos ahora vienen de hc.medico.
--              También elimina specialist_specialties que mapeaba
--              especialidades locales.
-- =================================================================

-- Remover constraints que referencian specialist_specialties
ALTER TABLE IF EXISTS appointments.specialist_specialties 
    DROP CONSTRAINT IF EXISTS specialist_specialties_specialist_id_fkey;

ALTER TABLE IF EXISTS appointments.specialist_specialties 
    DROP CONSTRAINT IF EXISTS specialist_specialties_specialty_id_fkey;

-- Eliminar tablas locales (ya no necesarias)
DROP TABLE IF EXISTS appointments.specialist_specialties;
DROP TABLE IF EXISTS appointments.specialists;

-- ============================================================
-- NOTA: Las especialidades se manejan mediante:
-- 1. specialist_metadata (JSONB) para mapeos temporales
-- 2. hc.medico.especialidad_principal para info básica
-- ============================================================

COMMENT ON TABLE appointments.specialist_metadata IS 
    'Tabla temporal de mapeo de especialidades por médico. Las especialidades '
    'se obtienen principalmente de hc.medico.especialidad_principal y se '
    'enriquecen aquí con metadatos de appointments si es necesario.';
