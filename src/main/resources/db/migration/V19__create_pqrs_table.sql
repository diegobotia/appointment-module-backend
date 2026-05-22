-- Tabla PQRS para el módulo de citas (idempotente en local y Supabase)

CREATE TABLE IF NOT EXISTS appointments.pqrs (
    id UUID PRIMARY KEY,
    cedula VARCHAR(10) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    descripcion TEXT NOT NULL,
    correo VARCHAR(100) NOT NULL,
    nombres VARCHAR(200),
    telefono VARCHAR(20),
    radicado VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'CREADO',
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_pqrs_cedula ON appointments.pqrs(cedula);
CREATE INDEX IF NOT EXISTS idx_pqrs_radicado ON appointments.pqrs(radicado);
CREATE INDEX IF NOT EXISTS idx_pqrs_status ON appointments.pqrs(status);
CREATE INDEX IF NOT EXISTS idx_pqrs_created_at ON appointments.pqrs(created_at);
