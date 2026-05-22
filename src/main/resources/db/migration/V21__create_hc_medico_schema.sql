-- =================================================================
-- V16: CREAR SCHEMA HC Y TABLA MEDICO
-- Fecha: 18 de mayo de 2026
-- Descripción: Crea el schema 'hc' (Health Center) con la tabla
--              de médicos. En Supabase real, este schema ya existe.
--              Para testing local, esta migración lo bootstrap.
-- =================================================================

-- Crear schema hc si no existe
CREATE SCHEMA IF NOT EXISTS hc;

-- Tabla: hc.medico (Información de Médicos)
CREATE TABLE IF NOT EXISTS hc.medico (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_medico VARCHAR(50) UNIQUE NOT NULL,
    primer_nombre VARCHAR(100) NOT NULL,
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    segundo_apellido VARCHAR(100),
    email VARCHAR(120) UNIQUE,
    telefono VARCHAR(30),
    especialidad_principal VARCHAR(100),
    numero_registro_medico VARCHAR(100) UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT hc_medico_email_valid CHECK (email IS NULL OR email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Índices para búsquedas comunes
CREATE INDEX IF NOT EXISTS idx_hc_medico_numero_medico ON hc.medico(numero_medico);
CREATE INDEX IF NOT EXISTS idx_hc_medico_email ON hc.medico(email);
CREATE INDEX IF NOT EXISTS idx_hc_medico_especialidad ON hc.medico(especialidad_principal);
CREATE INDEX IF NOT EXISTS idx_hc_medico_activo ON hc.medico(activo);

-- Insertar médicos de ejemplo para testing (si es DB local)
INSERT INTO hc.medico (
    numero_medico, 
    primer_nombre, 
    primer_apellido, 
    email, 
    especialidad_principal, 
    activo
)
VALUES
    ('MED001', 'Carlos', 'Rodriguez', 'carlos.rodriguez@ips.test', 'Fisiatria', true),
    ('MED002', 'Ana', 'Martinez', 'ana.martinez@ips.test', 'Dolor', true),
    ('MED003', 'Roberto', 'Lopez', 'roberto.lopez@ips.test', 'Medicina Laboral', true),
    ('MED004', 'Maria', 'Garcia', 'maria.garcia@ips.test', 'Psicologia', true),
    ('MED005', 'Juan', 'Hernandez', 'juan.hernandez@ips.test', 'Psiquiatria', true)
ON CONFLICT (numero_medico) DO NOTHING;

-- Comentarios para documentación
COMMENT ON TABLE hc.medico IS 'Tabla de médicos del sistema de health center. Fuente de verdad para información de médicos.';
COMMENT ON COLUMN hc.medico.numero_medico IS 'Identificador único del médico en el sistema';
COMMENT ON COLUMN hc.medico.especialidad_principal IS 'Especialidad primaria del médico (referencia informativa)';
