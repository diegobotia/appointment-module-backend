-- V32: Sede en plan y consultorio obligatorio en slots

ALTER TABLE appointments.schedule_plans
    ADD COLUMN IF NOT EXISTS sede_id INTEGER;

UPDATE appointments.schedule_plans
SET sede_id = 2
WHERE sede_id IS NULL;

ALTER TABLE appointments.schedule_plans
    ALTER COLUMN sede_id SET NOT NULL;

ALTER TABLE appointments.schedule_plans
    ADD CONSTRAINT fk_schedule_plans_sede
        FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_schedule_plans_sede_dates
    ON appointments.schedule_plans(sede_id, start_date, end_date);

ALTER TABLE appointments.schedule_plan_slots
    ALTER COLUMN consultorio_id SET NOT NULL;
