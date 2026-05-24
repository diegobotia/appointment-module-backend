-- =================================================================
-- V30: Vincular perfiles de login con médicos clínicos (hc.medicos)
-- Fecha: 2026-05-23
-- Descripción: core.profiles.medico_id referencia lógica a hc.medicos.id.
--              El JWT identifica profiles.id; citas y agendas usan medico_id.
-- =================================================================

ALTER TABLE core.profiles
    ADD COLUMN IF NOT EXISTS medico_id VARCHAR;

CREATE INDEX IF NOT EXISTS idx_profiles_medico_id ON core.profiles(medico_id);

COMMENT ON COLUMN core.profiles.medico_id IS
    'FK lógica hacia hc.medicos.id. Obligatorio para perfiles con rol Medico.';

-- Backfill legacy: perfiles cuyo id era también el id del médico en hc.medicos
UPDATE core.profiles p
SET medico_id = p.id::text
WHERE p.medico_id IS NULL
  AND EXISTS (
        SELECT 1
        FROM hc.medicos m
        WHERE m.id::text = p.id::text
  )
  AND p.role_id IN (
        SELECT r.id
        FROM core.roles r
        WHERE r.nombre IN ('Medico', 'MEDICO')
  );
