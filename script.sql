-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE core.admisiones (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  id_paciente uuid NOT NULL,
  id_entidad uuid,
  fecha_admision date NOT NULL,
  autorizacion character varying,
  id_tarifa uuid NOT NULL,
  tiene_copago boolean NOT NULL,
  tiene_consentimiento boolean NOT NULL,
  cod_tipo_usuario character varying NOT NULL,
  admitido_por character varying,
  CONSTRAINT admisiones_pkey PRIMARY KEY (id),
  CONSTRAINT admisiones_id_paciente_fkey FOREIGN KEY (id_paciente) REFERENCES core.pacientes(id),
  CONSTRAINT admisiones_id_entidad_fkey FOREIGN KEY (id_entidad) REFERENCES core.entidades(id),
  CONSTRAINT admisiones_id_tarifa_fkey FOREIGN KEY (id_tarifa) REFERENCES servicios_salud.tarifas(id),
  CONSTRAINT admisiones_cod_tipo_usuario_fkey FOREIGN KEY (cod_tipo_usuario) REFERENCES core.tipo_usuario(cod)
);
CREATE TABLE core.contacto (
  id uuid NOT NULL,
  email character varying UNIQUE,
  telefono character varying UNIQUE,
  CONSTRAINT contacto_pkey PRIMARY KEY (id)
);
CREATE TABLE core.departamento (
  codigo character varying NOT NULL,
  nombre character varying,
  CONSTRAINT departamento_pkey PRIMARY KEY (codigo)
);
CREATE TABLE core.direccion (
  id uuid NOT NULL,
  cod_municipio character varying NOT NULL,
  cod_zona_territorial character varying NOT NULL,
  detalle character varying NOT NULL,
  CONSTRAINT direccion_pkey PRIMARY KEY (id),
  CONSTRAINT direccion_codigo_municipio_fkey FOREIGN KEY (cod_municipio) REFERENCES core.municipio(cod),
  CONSTRAINT direccion_cod_zona_territorial_fkey FOREIGN KEY (cod_zona_territorial) REFERENCES core.zonas_territoriales(cod)
);
CREATE TABLE core.entidades (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  num_identificacion character varying UNIQUE,
  digito_verificacion character varying,
  razon_social character varying NOT NULL,
  id_direccion uuid,
  id_contacto uuid,
  CONSTRAINT entidades_pkey PRIMARY KEY (id)
);
CREATE TABLE core.escolaridades (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  descripcion character varying NOT NULL,
  CONSTRAINT escolaridades_pkey PRIMARY KEY (id)
);
CREATE TABLE core.estados_civil (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  descripcion character varying NOT NULL,
  CONSTRAINT estados_civil_pkey PRIMARY KEY (id)
);
CREATE TABLE core.generos (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  descripcion character varying NOT NULL,
  CONSTRAINT generos_pkey PRIMARY KEY (id)
);
CREATE TABLE core.grupos_sanguineos (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  descripcion character varying NOT NULL,
  CONSTRAINT grupos_sanguineos_pkey PRIMARY KEY (id)
);
CREATE TABLE core.municipio (
  cod character varying NOT NULL,
  nombre character varying,
  cod_departamento character varying,
  CONSTRAINT municipio_pkey PRIMARY KEY (cod),
  CONSTRAINT municipio_cod_departamento_fkey FOREIGN KEY (cod_departamento) REFERENCES core.departamento(codigo)
);
CREATE TABLE core.ocupaciones (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  descripcion character varying NOT NULL,
  CONSTRAINT ocupaciones_pkey PRIMARY KEY (id)
);
CREATE TABLE core.pacientes (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  nombre character varying NOT NULL,
  num_identificacion character varying NOT NULL,
  cod_tipo_identificacion character varying NOT NULL,
  fecha_nacimiento date NOT NULL,
  id_genero uuid NOT NULL,
  id_estado_civil uuid NOT NULL,
  id_ocupacion uuid NOT NULL,
  id_direccion uuid NOT NULL,
  id_contacto uuid NOT NULL,
  id_grupo_sanguineo uuid NOT NULL,
  id_escolaridad uuid NOT NULL,
  estrato smallint NOT NULL CHECK (estrato > 0 AND estrato < 7),
  id_pais_origen bigint NOT NULL,
  CONSTRAINT pacientes_pkey PRIMARY KEY (id),
  CONSTRAINT pacientes_id_direccion_fkey FOREIGN KEY (id_direccion) REFERENCES core.direccion(id),
  CONSTRAINT pacientes_id_contacto_fkey FOREIGN KEY (id_contacto) REFERENCES core.contacto(id),
  CONSTRAINT pacientes_cod_tipo_identificacion_fkey FOREIGN KEY (cod_tipo_identificacion) REFERENCES core.tipo_identificacion(codigo),
  CONSTRAINT pacientes_id_genero_fkey FOREIGN KEY (id_genero) REFERENCES core.generos(id),
  CONSTRAINT pacientes_id_estado_civil_fkey FOREIGN KEY (id_estado_civil) REFERENCES core.estados_civil(id),
  CONSTRAINT pacientes_id_ocupacion_fkey FOREIGN KEY (id_ocupacion) REFERENCES core.ocupaciones(id),
  CONSTRAINT pacientes_id_grupo_sanguineo_fkey FOREIGN KEY (id_grupo_sanguineo) REFERENCES core.grupos_sanguineos(id),
  CONSTRAINT pacientes_id_escolaridad_fkey FOREIGN KEY (id_escolaridad) REFERENCES core.escolaridades(id),
  CONSTRAINT pacientes_id_pais_origen_fkey FOREIGN KEY (id_pais_origen) REFERENCES core.paises(id)
);
CREATE TABLE core.paises (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  descripcion character varying,
  CONSTRAINT paises_pkey PRIMARY KEY (id)
);
CREATE TABLE core.tipo_identificacion (
  codigo character varying NOT NULL,
  descripcion character varying,
  cod_rips character varying,
  CONSTRAINT tipo_identificacion_pkey PRIMARY KEY (codigo)
);
CREATE TABLE core.tipo_usuario (
  cod character varying NOT NULL,
  descripcion character varying,
  CONSTRAINT tipo_usuario_pkey PRIMARY KEY (cod)
);
CREATE TABLE core.zonas_territoriales (
  cod character varying NOT NULL,
  descripcion character varying,
  CONSTRAINT zonas_territoriales_pkey PRIMARY KEY (cod)
);