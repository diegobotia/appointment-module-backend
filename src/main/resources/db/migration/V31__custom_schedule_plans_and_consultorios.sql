-- =====================================================================
-- V31: Planes de Horarios con Fechas Flexibles y Consultorios Fijos
-- =====================================================================

-- 1. Agregar columnas de fecha a schedule_plans
ALTER TABLE appointments.schedule_plans ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE appointments.schedule_plans ADD COLUMN IF NOT EXISTS end_date DATE;

-- 2. Migrar registros existentes mapeando periodos trimestrales a fechas reales
UPDATE appointments.schedule_plans
SET start_date = CASE 
        WHEN plan_quarter = 1 THEN (plan_year || '-01-01')::DATE
        WHEN plan_quarter = 2 THEN (plan_year || '-04-01')::DATE
        WHEN plan_quarter = 3 THEN (plan_year || '-07-01')::DATE
        WHEN plan_quarter = 4 THEN (plan_year || '-10-01')::DATE
    END,
    end_date = CASE 
        WHEN plan_quarter = 1 THEN (plan_year || '-03-31')::DATE
        WHEN plan_quarter = 2 THEN (plan_year || '-06-30')::DATE
        WHEN plan_quarter = 3 THEN (plan_year || '-09-30')::DATE
        WHEN plan_quarter = 4 THEN (plan_year || '-12-31')::DATE
    END
WHERE start_date IS NULL OR end_date IS NULL;

-- 3. Hacer las columnas de fecha obligatorias
ALTER TABLE appointments.schedule_plans ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE appointments.schedule_plans ALTER COLUMN end_date SET NOT NULL;

-- 4. Agregar columna consultorio_id a schedule_plan_slots y schedules
ALTER TABLE appointments.schedule_plan_slots ADD COLUMN IF NOT EXISTS consultorio_id UUID REFERENCES appointments.facility_resources(id) ON DELETE SET NULL;
ALTER TABLE appointments.schedules ADD COLUMN IF NOT EXISTS consultorio_id UUID REFERENCES appointments.facility_resources(id) ON DELETE SET NULL;

-- 5. Eliminar restricciones e índices obsoletos
ALTER TABLE appointments.schedule_plans DROP CONSTRAINT IF EXISTS uq_schedule_plan_version;
DROP INDEX IF EXISTS appointments.uq_schedule_plan_active_period;
DROP INDEX IF EXISTS appointments.idx_schedule_plans_specialist_period;
DROP INDEX IF EXISTS appointments.idx_schedule_plans_specialist;

-- 6. Crear nuevos índices y restricciones para mantener integridad referencial y de periodos
ALTER TABLE appointments.schedule_plans ADD CONSTRAINT uq_schedule_plan_version UNIQUE (specialist_id, start_date, end_date, version_number);
CREATE INDEX IF NOT EXISTS idx_schedule_plans_specialist_dates ON appointments.schedule_plans(specialist_id, start_date, end_date);
CREATE UNIQUE INDEX IF NOT EXISTS uq_schedule_plan_active_period ON appointments.schedule_plans(specialist_id, start_date, end_date)
    WHERE is_active_version = true;

-- 7. Eliminar columnas redundantes obsoletas
ALTER TABLE appointments.schedule_plans DROP COLUMN IF EXISTS plan_year;
ALTER TABLE appointments.schedule_plans DROP COLUMN IF EXISTS plan_quarter;
