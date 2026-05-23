-- =====================================================================
-- V25: Inventario físico de recursos por sede
-- =====================================================================

CREATE TABLE IF NOT EXISTS appointments.facility_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL,
    resource_type VARCHAR(32) NOT NULL CHECK (
        resource_type IN ('CONSULTORIO', 'FISIOTERAPIA', 'TERAPIA_OCUPACIONAL', 'REUNION_STAFF')
    ),
    code VARCHAR(40) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    capacity_units SMALLINT NOT NULL DEFAULT 1 CHECK (capacity_units > 0),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_facility_resources_facility_code UNIQUE (facility_id, code),
    CONSTRAINT fk_facility_resources_facility
        FOREIGN KEY (facility_id) REFERENCES appointments.facilities(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_facility_resources_facility_type_active
    ON appointments.facility_resources(facility_id, resource_type, is_active);

CREATE OR REPLACE FUNCTION appointments.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_facility_resources_updated_at ON appointments.facility_resources;
CREATE TRIGGER trg_facility_resources_updated_at
    BEFORE UPDATE ON appointments.facility_resources
    FOR EACH ROW
    EXECUTE FUNCTION appointments.update_updated_at_column();

-- Sede Conquistadores: 4 consultorios, 2 fisio, 1 terapia ocupacional
INSERT INTO appointments.facility_resources (facility_id, resource_type, code, display_name, capacity_units, is_active)
SELECT f.id, r.resource_type, r.code, r.display_name, r.capacity_units, true
FROM appointments.facilities f
CROSS JOIN (
    VALUES
        ('CONSULTORIO', 'CONQ-CONS-01', 'Consultorio 1', 1),
        ('CONSULTORIO', 'CONQ-CONS-02', 'Consultorio 2', 1),
        ('CONSULTORIO', 'CONQ-CONS-03', 'Consultorio 3', 1),
        ('CONSULTORIO', 'CONQ-CONS-04', 'Consultorio 4', 1),
        ('FISIOTERAPIA', 'CONQ-FISIO-01', 'Sala fisioterapia 1', 1),
        ('FISIOTERAPIA', 'CONQ-FISIO-02', 'Sala fisioterapia 2', 1),
        ('TERAPIA_OCUPACIONAL', 'CONQ-TO-01', 'Sala terapia ocupacional', 1)
) AS r(resource_type, code, display_name, capacity_units)
WHERE f.code = 'SEDE_CONQUISTADORES'
ON CONFLICT (facility_id, code) DO NOTHING;

-- Sede Belén: 1 consultorio, 1 fisio, 1 terapia ocupacional
INSERT INTO appointments.facility_resources (facility_id, resource_type, code, display_name, capacity_units, is_active)
SELECT f.id, r.resource_type, r.code, r.display_name, r.capacity_units, true
FROM appointments.facilities f
CROSS JOIN (
    VALUES
        ('CONSULTORIO', 'BEL-CONS-01', 'Consultorio 1', 1),
        ('FISIOTERAPIA', 'BEL-FISIO-01', 'Sala fisioterapia 1', 1),
        ('TERAPIA_OCUPACIONAL', 'BEL-TO-01', 'Sala terapia ocupacional', 1)
) AS r(resource_type, code, display_name, capacity_units)
WHERE f.code = 'SEDE_BELEN'
ON CONFLICT (facility_id, code) DO NOTHING;
