-- V12__create_pqrs_table.sql

CREATE TABLE pqrs (
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
CREATE INDEX idx_pqrs_cedula ON pqrs(cedula);
CREATE INDEX idx_pqrs_radicado ON pqrs(radicado);
CREATE INDEX idx_pqrs_status ON pqrs(status);
CREATE INDEX idx_pqrs_created_at ON pqrs(created_at);
