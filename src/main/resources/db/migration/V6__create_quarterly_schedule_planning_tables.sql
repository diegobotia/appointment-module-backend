-- =============================================
-- V6: PLANIFICACION TRIMESTRAL, SLOTS Y BLOQUEOS
-- =============================================

CREATE TABLE IF NOT EXISTS schedule_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    specialist_id UUID NOT NULL REFERENCES specialists(id) ON DELETE CASCADE,
    plan_year INTEGER NOT NULL,
    plan_quarter INTEGER NOT NULL CHECK (plan_quarter BETWEEN 1 AND 4),
    version_number INTEGER NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT false,
    is_active_version BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_schedule_plan_version UNIQUE (specialist_id, plan_year, plan_quarter, version_number)
);

CREATE TABLE IF NOT EXISTS schedule_plan_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_plan_id UUID NOT NULL REFERENCES schedule_plans(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL,
    max_patients_per_slot INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_plan_slot_time_range CHECK (end_time > start_time),
    CONSTRAINT chk_schedule_plan_slot_duration CHECK (slot_duration_minutes > 0),
    CONSTRAINT chk_schedule_plan_slot_capacity CHECK (max_patients_per_slot > 0)
);

CREATE TABLE IF NOT EXISTS schedule_plan_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_plan_id UUID NOT NULL REFERENCES schedule_plans(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(255),
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_plan_block_date_range CHECK (end_date >= start_date),
    CONSTRAINT chk_schedule_plan_block_time_range CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_schedule_plans_specialist_period ON schedule_plans(specialist_id, plan_year, plan_quarter);
CREATE UNIQUE INDEX IF NOT EXISTS uq_schedule_plan_active_period ON schedule_plans(specialist_id, plan_year, plan_quarter)
    WHERE is_active_version = true;
CREATE INDEX IF NOT EXISTS idx_schedule_plan_slots_plan ON schedule_plan_slots(schedule_plan_id, day_of_week);
CREATE INDEX IF NOT EXISTS idx_schedule_plan_blocks_plan_date ON schedule_plan_blocks(schedule_plan_id, start_date, end_date);

DROP TRIGGER IF EXISTS trg_schedule_plans_updated_at ON schedule_plans;
CREATE TRIGGER trg_schedule_plans_updated_at
    BEFORE UPDATE ON schedule_plans
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
