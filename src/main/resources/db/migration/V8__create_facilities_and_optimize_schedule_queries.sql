-- =============================================
-- V8: SEDES (FACILITIES) Y OPTIMIZACION DE CONSULTAS POR SEDE
-- =============================================

CREATE TABLE IF NOT EXISTS facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_facilities_active ON facilities(is_active);

INSERT INTO facilities (code, name, address, is_active)
VALUES
    ('SEDE_PRINCIPAL', 'Sede Principal', 'Direccion pendiente - Sede Principal', true),
    ('SEDE_NORTE', 'Sede Norte', 'Direccion pendiente - Sede Norte', true)
ON CONFLICT (code) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_schedules_doctor_facility_day ON schedules(doctor_id, facility_id, day_of_week);

DROP TRIGGER IF EXISTS trg_facilities_updated_at ON facilities;
CREATE TRIGGER trg_facilities_updated_at
    BEFORE UPDATE ON facilities
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
