-- =====================================================================
-- V24: Horario institucional de sede + actualización maestros de facilities
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Alias de códigos legacy (compatibilidad n8n / integraciones)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments.facility_code_aliases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL,
    alias_code VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_facility_code_aliases_code UNIQUE (alias_code),
    CONSTRAINT fk_facility_code_aliases_facility
        FOREIGN KEY (facility_id) REFERENCES appointments.facilities(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_facility_code_aliases_facility
    ON appointments.facility_code_aliases(facility_id);

-- ---------------------------------------------------------------------
-- 2. Actualizar sedes: nombres, direcciones y códigos canónicos
-- ---------------------------------------------------------------------
UPDATE appointments.facilities
SET
    code = 'SEDE_CONQUISTADORES',
    name = 'Sede Conquistadores',
    address = 'Calle 34 # 63-56, barrio Conquistadores'
WHERE code = 'SEDE_PRINCIPAL';

UPDATE appointments.facilities
SET
    code = 'SEDE_BELEN',
    name = 'Sede Belén',
    address = 'Cra 82A # 28-39, Belén los Alpes'
WHERE code = 'SEDE_NORTE';

INSERT INTO appointments.facility_code_aliases (facility_id, alias_code)
SELECT f.id, 'SEDE_PRINCIPAL'
FROM appointments.facilities f
WHERE f.code = 'SEDE_CONQUISTADORES'
ON CONFLICT (alias_code) DO NOTHING;

INSERT INTO appointments.facility_code_aliases (facility_id, alias_code)
SELECT f.id, 'SEDE_NORTE'
FROM appointments.facilities f
WHERE f.code = 'SEDE_BELEN'
ON CONFLICT (alias_code) DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. Horario de atención por sede (day_of_week ISO-8601: 1=Lunes … 7=Domingo)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments.facility_operating_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    open_time TIME,
    close_time TIME,
    is_closed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_facility_operating_hours_facility_day UNIQUE (facility_id, day_of_week),
    CONSTRAINT chk_facility_operating_hours_window
        CHECK (
            is_closed = true
            OR (open_time IS NOT NULL AND close_time IS NOT NULL AND open_time < close_time)
        ),
    CONSTRAINT fk_facility_operating_hours_facility
        FOREIGN KEY (facility_id) REFERENCES appointments.facilities(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_facility_operating_hours_facility
    ON appointments.facility_operating_hours(facility_id);

-- Función en schema appointments (V1–V11 no corren en Supabase con baseline 11)
CREATE OR REPLACE FUNCTION appointments.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_facility_operating_hours_updated_at ON appointments.facility_operating_hours;
CREATE TRIGGER trg_facility_operating_hours_updated_at
    BEFORE UPDATE ON appointments.facility_operating_hours
    FOR EACH ROW
    EXECUTE FUNCTION appointments.update_updated_at_column();

-- Lunes–Viernes 07:00–18:00; Sábado 08:00–12:00; Domingo cerrado
INSERT INTO appointments.facility_operating_hours (facility_id, day_of_week, open_time, close_time, is_closed)
SELECT f.id, d.day_of_week, d.open_time, d.close_time, d.is_closed
FROM appointments.facilities f
CROSS JOIN (
    VALUES
        (1, TIME '07:00', TIME '18:00', false),
        (2, TIME '07:00', TIME '18:00', false),
        (3, TIME '07:00', TIME '18:00', false),
        (4, TIME '07:00', TIME '18:00', false),
        (5, TIME '07:00', TIME '18:00', false),
        (6, TIME '08:00', TIME '12:00', false),
        (7, NULL::TIME, NULL::TIME, true)
) AS d(day_of_week, open_time, close_time, is_closed)
WHERE f.code IN ('SEDE_CONQUISTADORES', 'SEDE_BELEN')
ON CONFLICT (facility_id, day_of_week) DO NOTHING;
