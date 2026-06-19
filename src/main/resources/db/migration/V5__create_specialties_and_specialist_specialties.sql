-- =============================================
-- V5: ESPECIALIDADES Y RELACION ESPECIALISTA-ESPECIALIDAD
-- =============================================

CREATE TABLE IF NOT EXISTS specialties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS specialist_specialties (
    specialist_id UUID NOT NULL REFERENCES specialists(id) ON DELETE CASCADE,
    specialty_id UUID NOT NULL REFERENCES specialties(id) ON DELETE CASCADE,
    PRIMARY KEY (specialist_id, specialty_id)
);

CREATE INDEX IF NOT EXISTS idx_specialist_specialties_specialty_id ON specialist_specialties(specialty_id);
CREATE INDEX IF NOT EXISTS idx_specialties_active ON specialties(is_active);

INSERT INTO specialties (code, display_name, is_active)
VALUES
    ('FISIATRIA', 'Fisiatria', true),
    ('FISIATRIA_INTEGRAL_DOLOR', 'Fisiatria integral del dolor', true),
    ('DOLOR', 'Dolor', true),
    ('MEDICINA_LABORAL', 'Medicina laboral', true),
    ('PSICOLOGIA', 'Psicologia', true),
    ('PSIQUIATRIA', 'Psiquiatria', true),
    ('ELECTROMIOGRAFIA', 'Electromiografia', true),
    ('TERAPIA_FISICA', 'Terapia fisica', true),
    ('TERAPIA_OCUPACIONAL', 'Terapia ocupacional', true)
ON CONFLICT (code) DO NOTHING;