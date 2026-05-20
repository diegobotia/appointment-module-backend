-- =================================================================
-- V0: BOOTSTRAP LOCAL DE SCHEMA CORE
-- Fecha: 2026-05-16
-- Descripción: Provee un baseline local mínimo para permitir validar
--              el módulo de appointments contra PostgreSQL limpio.
--              En Supabase real, las tablas ya existen y los IF NOT EXISTS
--              evitan impactos.
-- =================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS core;

CREATE TABLE IF NOT EXISTS core.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS core.contacto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(120) UNIQUE,
    telefono VARCHAR(30) UNIQUE
);

CREATE TABLE IF NOT EXISTS core.direccion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cod_municipio VARCHAR(20) NOT NULL,
    cod_zona_territorial VARCHAR(20) NOT NULL,
    detalle VARCHAR(255) NOT NULL,
    barrio VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS core.profiles (
    id UUID PRIMARY KEY,
    role_id UUID NOT NULL REFERENCES core.roles(id),
    name VARCHAR(120),
    email VARCHAR(120) UNIQUE,
    password_change_required BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS core.pacientes (
    id UUID PRIMARY KEY,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    num_identificacion VARCHAR(50) NOT NULL UNIQUE,
    cod_tipo_identificacion VARCHAR(20) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    id_genero UUID NOT NULL,
    id_estado_civil UUID NOT NULL,
    id_ocupacion UUID NOT NULL,
    id_direccion UUID NOT NULL,
    id_contacto UUID NOT NULL,
    id_grupo_sanguineo UUID NOT NULL,
    id_escolaridad UUID NOT NULL,
    estrato INTEGER NOT NULL,
    id_pais_origen BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS core.admisiones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid()
);

INSERT INTO core.roles (nombre, descripcion)
VALUES
    ('Medico', 'Gestiona historia clínica'),
    ('Admisiones', 'Gestiona admisiones'),
    ('Administracion', 'Acceso total al sistema'),
    ('Facturacion', 'Gestiona cuentas médicas')
ON CONFLICT (nombre) DO NOTHING;
