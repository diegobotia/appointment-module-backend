CREATE SCHEMA IF NOT EXISTS appointments;

CREATE SCHEMA IF NOT EXISTS core;

CREATE SCHEMA IF NOT EXISTS hc;

CREATE OR REPLACE FUNCTION appointments.get_confirmed_appointments_by_patient(p_patient_id uuid, p_sede_id integer DEFAULT NULL::integer) RETURNS TABLE(id uuid, patient_id uuid, appointment_date date, appointment_time time without time zone, appointment_type character varying, specialty character varying, doctor_id uuid, doctor_name text, sede_id integer, status character varying)
    LANGUAGE sql
    AS $$
  SELECT 
    a.id, a.patient_id, a.appointment_date, a.appointment_time,
    a.appointment_type, a.specialty, a.doctor_id,
    COALESCE(m.nombre || ' ' || m.apellido, 'Sin asignar') AS doctor_name,
    a.sede_id, a.status
  FROM appointments.appointments a
  LEFT JOIN hc.medicos m ON a.doctor_id = m.id::text
  WHERE a.patient_id = p_patient_id
    AND a.status = 'CONFIRMED'
    AND (p_sede_id IS NULL OR a.sede_id = p_sede_id)
  ORDER BY a.appointment_date ASC, a.appointment_time ASC;
$$;

CREATE OR REPLACE FUNCTION appointments.log_status_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO appointments.appointment_state_history 
        (appointment_id, old_status, new_status, changed_at)
        VALUES (NEW.id, OLD.status, NEW.status, CURRENT_TIMESTAMP);
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION appointments.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION appointments.validate_appointment_facility() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.schedule_id IS NOT NULL THEN
        IF NEW.sede_id IS DISTINCT FROM (
            SELECT sede_id FROM appointments.schedules WHERE id = NEW.schedule_id
        ) THEN
            RAISE EXCEPTION 'Sede mismatch between appointment and schedule';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION hc.actualizar_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TABLE IF NOT EXISTS appointments.appointment_audit_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    appointment_id uuid NOT NULL,
    action character varying(50) NOT NULL,
    old_values jsonb,
    new_values jsonb,
    performed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    performed_by uuid,
    ip_address character varying(255)
);

CREATE TABLE IF NOT EXISTS appointments.appointment_participants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    appointment_id uuid NOT NULL,
    doctor_id character varying(100) NOT NULL,
    participant_order integer NOT NULL,
    participant_role character varying(20) NOT NULL,
    confirmed_at timestamp without time zone,
    CONSTRAINT appointment_participants_participant_order_check CHECK (((participant_order >= 1) AND (participant_order <= 4))),
    CONSTRAINT appointment_participants_participant_role_check CHECK (((participant_role)::text = ANY (ARRAY[('PRIMARY'::character varying)::text, ('SECONDARY'::character varying)::text, ('TERTIARY'::character varying)::text, ('QUATERNARY'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS appointments.appointment_resource_allocations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    appointment_id uuid NOT NULL,
    resource_type character varying(32) NOT NULL,
    facility_resource_id uuid,
    appointment_date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    capacity_session_key character varying(160) NOT NULL,
    released_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    sede_id integer NOT NULL,
    CONSTRAINT appointment_resource_allocations_resource_type_check CHECK (((resource_type)::text = ANY (ARRAY[('CONSULTORIO'::character varying)::text, ('FISIOTERAPIA'::character varying)::text, ('TERAPIA_OCUPACIONAL'::character varying)::text, ('REUNION_STAFF'::character varying)::text]))),
    CONSTRAINT chk_resource_alloc_time_window CHECK ((start_time < end_time))
);

CREATE TABLE IF NOT EXISTS appointments.appointment_state_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    appointment_id uuid NOT NULL,
    old_status character varying(50),
    new_status character varying(50) NOT NULL,
    changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    changed_by uuid,
    reason character varying(500),
    metadata jsonb
);

CREATE TABLE IF NOT EXISTS appointments.appointments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    patient_id uuid,
    schedule_id uuid,
    appointment_date date NOT NULL,
    appointment_time time without time zone NOT NULL,
    duration_minutes integer DEFAULT 30 NOT NULL,
    appointment_type character varying(50) NOT NULL,
    status character varying(50) DEFAULT 'SCHEDULED'::character varying NOT NULL,
    specialty character varying(100),
    reason text,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    confirmed_at timestamp without time zone,
    cancelled_at timestamp without time zone,
    cancellation_reason text,
    booking_channel character varying(255) NOT NULL,
    n8n_conversation_id character varying(255),
    sede_id integer NOT NULL,
    doctor_id character varying(64),
    CONSTRAINT appointments_appointment_type_check CHECK (((appointment_type)::text = ANY (ARRAY[('PRESENCIAL'::character varying)::text, ('JUNTA_MEDICA'::character varying)::text, ('TERAPIA_FISICA'::character varying)::text, ('TERAPIA_OCUPACIONAL'::character varying)::text, ('STAFF'::character varying)::text, ('BLOQUEO'::character varying)::text]))),
    CONSTRAINT appointments_booking_channel_check CHECK (((booking_channel)::text = ANY (ARRAY[('N8N'::character varying)::text, ('STAFF'::character varying)::text]))),
    CONSTRAINT appointments_status_check CHECK (((status)::text = ANY (ARRAY[('SCHEDULED'::character varying)::text, ('PENDIENTE_CONFIRMACION_GRUPO'::character varying)::text, ('CONFIRMED'::character varying)::text, ('CHECKED_IN'::character varying)::text, ('COMPLETED'::character varying)::text, ('CANCELLED'::character varying)::text, ('NO_SHOW'::character varying)::text]))),
    CONSTRAINT chk_future_appointment CHECK (((appointment_date > CURRENT_DATE) OR ((appointment_date = CURRENT_DATE) AND ((appointment_time)::time with time zone > CURRENT_TIME))))
);

CREATE TABLE IF NOT EXISTS appointments.domain_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_type character varying(100) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_data jsonb NOT NULL,
    occurred_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    published boolean DEFAULT false NOT NULL,
    published_at timestamp without time zone
);

CREATE TABLE IF NOT EXISTS appointments.facility_operating_hours (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    day_of_week smallint NOT NULL,
    open_time time without time zone,
    close_time time without time zone,
    is_closed boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    sede_id integer NOT NULL,
    CONSTRAINT chk_facility_operating_hours_window CHECK (((is_closed = true) OR ((open_time IS NOT NULL) AND (close_time IS NOT NULL) AND (open_time < close_time)))),
    CONSTRAINT facility_operating_hours_day_of_week_check CHECK (((day_of_week >= 1) AND (day_of_week <= 7)))
);

CREATE TABLE IF NOT EXISTS appointments.facility_resources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    resource_type character varying(32) NOT NULL,
    code character varying(40) NOT NULL,
    display_name character varying(120) NOT NULL,
    capacity_units smallint DEFAULT 1 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    sede_id integer NOT NULL,
    CONSTRAINT facility_resources_capacity_units_check CHECK ((capacity_units > 0)),
    CONSTRAINT facility_resources_resource_type_check CHECK (((resource_type)::text = ANY (ARRAY[('CONSULTORIO'::character varying)::text, ('FISIOTERAPIA'::character varying)::text, ('TERAPIA_OCUPACIONAL'::character varying)::text, ('REUNION_STAFF'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS appointments.n8n_idempotency_keys (
    id uuid NOT NULL,
    appointment_id uuid,
    created_at timestamp(6) without time zone NOT NULL,
    idempotency_key character varying(255) NOT NULL,
    scope character varying(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS appointments.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    notification_type character varying(20) NOT NULL,
    recipient character varying(120) NOT NULL,
    message_content text,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    sent_at timestamp without time zone,
    failure_reason text,
    entity_id uuid NOT NULL,
    entity_type character varying(255) NOT NULL,
    last_attempt_at timestamp(6) without time zone,
    purpose character varying(255) NOT NULL,
    retry_count integer NOT NULL,
    CONSTRAINT notifications_entity_type_check CHECK (((entity_type)::text = ANY (ARRAY[('APPOINTMENT'::character varying)::text, ('PATIENT'::character varying)::text]))),
    CONSTRAINT notifications_notification_type_check CHECK (((notification_type)::text = ANY (ARRAY[('SMS'::character varying)::text, ('WHATSAPP'::character varying)::text, ('EMAIL'::character varying)::text]))),
    CONSTRAINT notifications_purpose_check CHECK (((purpose)::text = ANY (ARRAY[('APPOINTMENT_CREATED'::character varying)::text, ('APPOINTMENT_CANCELLED'::character varying)::text, ('REMINDER_24H'::character varying)::text, ('REMINDER_2H'::character varying)::text, ('MANUAL_RETRY'::character varying)::text]))),
    CONSTRAINT notifications_status_check CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('SENT'::character varying)::text, ('FAILED'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS appointments.pqrs (
    id uuid NOT NULL,
    cedula character varying(255) NOT NULL,
    correo character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    descripcion character varying(2000) NOT NULL,
    metadata text,
    nombres character varying(255),
    radicado character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    telefono character varying(255),
    tipo character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT pqrs_status_check CHECK (((status)::text = ANY (ARRAY[('CREADO'::character varying)::text, ('EN_REVISION'::character varying)::text, ('RESPONDIDO'::character varying)::text, ('CERRADO'::character varying)::text]))),
    CONSTRAINT pqrs_tipo_check CHECK (((tipo)::text = ANY (ARRAY[('PETICION'::character varying)::text, ('QUEJA'::character varying)::text, ('RECLAMO'::character varying)::text, ('SUGERENCIA'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS appointments.schedule_blocks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    schedule_id uuid NOT NULL,
    block_date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    reason character varying(200),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by uuid,
    CONSTRAINT chk_schedule_blocks_time_range CHECK ((end_time > start_time))
);

CREATE TABLE IF NOT EXISTS appointments.schedule_plan_blocks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    schedule_plan_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    reason character varying(255),
    created_by uuid,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_schedule_plan_block_date_range CHECK ((end_date >= start_date)),
    CONSTRAINT chk_schedule_plan_block_time_range CHECK ((end_time > start_time))
);

CREATE TABLE IF NOT EXISTS appointments.schedule_plan_slots (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    schedule_plan_id uuid NOT NULL,
    day_of_week integer NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    slot_duration_minutes integer NOT NULL,
    max_patients_per_slot integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    consultorio_id uuid NOT NULL,
    CONSTRAINT chk_schedule_plan_slot_capacity CHECK ((max_patients_per_slot > 0)),
    CONSTRAINT chk_schedule_plan_slot_duration CHECK ((slot_duration_minutes > 0)),
    CONSTRAINT chk_schedule_plan_slot_time_range CHECK ((end_time > start_time)),
    CONSTRAINT schedule_plan_slots_day_of_week_check CHECK (((day_of_week >= 1) AND (day_of_week <= 7)))
);

CREATE TABLE IF NOT EXISTS appointments.schedule_plans (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    specialist_id character varying(100) NOT NULL,
    version_number integer NOT NULL,
    is_published boolean DEFAULT false NOT NULL,
    is_active_version boolean DEFAULT false NOT NULL,
    published_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    start_date date NOT NULL,
    end_date date NOT NULL,
    sede_id integer NOT NULL
);

CREATE TABLE IF NOT EXISTS appointments.schedules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    doctor_id character varying(100) NOT NULL,
    specialty character varying(100),
    day_of_week integer NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    slot_duration_minutes integer DEFAULT 30 NOT NULL,
    max_patients_per_slot integer DEFAULT 1 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    sede_id integer NOT NULL,
    consultorio_id uuid,
    CONSTRAINT chk_schedules_time_range CHECK ((end_time > start_time)),
    CONSTRAINT schedules_day_of_week_check CHECK (((day_of_week >= 1) AND (day_of_week <= 7)))
);

CREATE TABLE IF NOT EXISTS appointments.sede_code_aliases (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sede_id integer NOT NULL,
    alias_code character varying(40) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS appointments.specialist_metadata (
    profile_id uuid NOT NULL,
    specialties_json jsonb DEFAULT '{"primary": null, "available": [], "secondary": []}'::jsonb NOT NULL,
    max_patients_per_slot integer DEFAULT 1 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    synced_from_hc boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT chk_specialist_metadata_max_patients CHECK (((max_patients_per_slot >= 1) AND (max_patients_per_slot <= 10)))
);

CREATE TABLE IF NOT EXISTS core.admisiones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    id_paciente uuid NOT NULL,
    id_entidad uuid,
    fecha_admision date NOT NULL,
    autorizacion uuid,
    tiene_consentimiento boolean NOT NULL,
    admitido_por uuid,
    id_contacto_paciente uuid,
    hora_admision time without time zone DEFAULT now() NOT NULL,
    firma_asistencia_archivo_id uuid,
    firma_asistencia_biometria_id uuid
);

CREATE TABLE IF NOT EXISTS core.appointment_admissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    appointment_id uuid NOT NULL,
    admission_id uuid NOT NULL,
    linked_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    linked_by uuid,
    notes text
);

CREATE TABLE IF NOT EXISTS core.archivos (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    bucket character varying(255) NOT NULL,
    path character varying(255) NOT NULL,
    nombre_original character varying(255),
    content_type character varying(100),
    tamano_bytes bigint,
    subido_por uuid,
    subido_en timestamp without time zone DEFAULT now(),
    temporal boolean DEFAULT false NOT NULL
);

CREATE TABLE IF NOT EXISTS core.autorizaciones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    num_autorizacion character varying(255) NOT NULL,
    servicio_autorizado uuid NOT NULL,
    cantidad_autorizada smallint NOT NULL,
    cantidad_utilizada smallint NOT NULL,
    id_entidad uuid NOT NULL,
    autorizacion_archivo_id uuid
);

CREATE TABLE IF NOT EXISTS core.coberturas_salud (
    cod character varying NOT NULL,
    descripcion character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.conceptos_recaudo (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cod_tipo_recaudo character varying(255) NOT NULL,
    valor numeric,
    recaudo_pendiente boolean DEFAULT true NOT NULL,
    recaudo_error text,
    cod_medio_pago integer,
    id_banco uuid,
    referencia text,
    comprobante_archivo_id uuid,
    nota text
);

CREATE TABLE IF NOT EXISTS core.contacto (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255),
    telefono character varying(255)
);

CREATE TABLE IF NOT EXISTS core.contactos_paciente (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    id_paciente uuid NOT NULL,
    id_parentesco uuid NOT NULL,
    nombre character varying(150) NOT NULL,
    telefono character varying(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS core.cuentas_medicas (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    id_paciente uuid NOT NULL,
    id_entidad uuid NOT NULL,
    id_autorizacion uuid,
    fecha_apertura date NOT NULL,
    abierta_por uuid NOT NULL,
    estado character varying NOT NULL,
    id_recaudo uuid,
    id_sede integer NOT NULL,
    num_poliza character varying(255),
    consentimiento_archivo_id uuid,
    consentimiento_biometria_id uuid,
    motivo_cierre character varying
);

CREATE TABLE IF NOT EXISTS core.departamento (
    codigo character varying(255) NOT NULL,
    nombre character varying(255)
);

CREATE TABLE IF NOT EXISTS core.direccion (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cod_municipio character varying(255) NOT NULL,
    cod_zona_territorial character varying(255) NOT NULL,
    detalle character varying(255) NOT NULL,
    barrio character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS core.entidades (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    num_identificacion character varying(255) NOT NULL,
    digito_verificacion character varying(255),
    razon_social character varying(255) NOT NULL,
    id_direccion uuid,
    id_contacto uuid,
    id_tipo_usuario character varying(255),
    cod_tipo_identificacion character varying(255) DEFAULT '31'::character varying NOT NULL,
    cod_responsabilidad_tributaria character varying(255),
    esta_activo boolean DEFAULT true NOT NULL,
    numero_contrato character varying(255),
    es_poliza boolean DEFAULT false NOT NULL,
    cod_cobertura_salud character varying,
    tipo_facturacion character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.escolaridades (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    descripcion character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.estados_civil (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    descripcion character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.estados_cuenta_medica (
    codigo character varying NOT NULL,
    nombre character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.generos (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    descripcion character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.grupos_sanguineos (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    descripcion character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.item_cuenta_medica (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    id_cuenta_medica uuid,
    id_admision uuid,
    cantidad bigint NOT NULL,
    valor_unitario numeric(38,2) NOT NULL,
    id_procedimiento uuid
);

CREATE TABLE IF NOT EXISTS core.municipio (
    cod character varying(255) NOT NULL,
    nombre character varying(255),
    cod_departamento character varying(255)
);

CREATE TABLE IF NOT EXISTS core.ocupaciones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    descripcion character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.pacientes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombres character varying(255) NOT NULL,
    num_identificacion character varying(255) NOT NULL,
    cod_tipo_identificacion character varying(255) NOT NULL,
    fecha_nacimiento date NOT NULL,
    id_genero uuid NOT NULL,
    id_estado_civil uuid NOT NULL,
    id_ocupacion uuid NOT NULL,
    id_direccion uuid NOT NULL,
    id_contacto uuid NOT NULL,
    id_grupo_sanguineo uuid NOT NULL,
    id_escolaridad uuid NOT NULL,
    estrato smallint NOT NULL,
    id_pais_origen bigint NOT NULL,
    apellidos character varying(255) NOT NULL,
    id_entidad uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    created_by uuid,
    updated_by uuid,
    CONSTRAINT pacientes_estrato_check CHECK (((estrato > 0) AND (estrato < 7)))
);

CREATE TABLE IF NOT EXISTS core.pacientes_audit_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    paciente_id uuid NOT NULL,
    action character varying NOT NULL,
    old_values jsonb,
    new_values jsonb,
    performed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    performed_by uuid,
    ip_address character varying
);

CREATE TABLE IF NOT EXISTS core.paises (
    id bigint NOT NULL,
    descripcion character varying
);

ALTER TABLE core.paises ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME core.paises_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE IF NOT EXISTS core.parentescos (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS core.profiles (
    id uuid NOT NULL,
    role_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    name character varying,
    password_change_required boolean,
    email character varying,
    esta_activo boolean DEFAULT true NOT NULL,
    medico_id uuid,
    password_hash character varying(255)
);

CREATE TABLE IF NOT EXISTS core.refresh_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    profile_id uuid NOT NULL,
    token_hash character varying(255) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    revoked boolean DEFAULT false NOT NULL
);

CREATE TABLE IF NOT EXISTS core.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying NOT NULL,
    descripcion character varying
);

CREATE TABLE IF NOT EXISTS core.sede (
    id integer NOT NULL,
    nombre character varying(255) NOT NULL,
    direccion uuid NOT NULL,
    matricula_mercantil character varying(255) NOT NULL
);

ALTER TABLE core.sede ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME core.sede_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE IF NOT EXISTS core.tipo_identificacion (
    codigo character varying NOT NULL,
    descripcion character varying,
    cod_rips character varying
);

CREATE TABLE IF NOT EXISTS core.tipo_recaudo (
    codigo character varying NOT NULL,
    descripcion character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.tipo_usuario (
    cod character varying NOT NULL,
    descripcion character varying
);

CREATE TABLE IF NOT EXISTS core.tipos_facturacion (
    codigo character varying NOT NULL,
    nombre character varying NOT NULL
);

CREATE TABLE IF NOT EXISTS core.zonas_territoriales (
    cod character varying NOT NULL,
    descripcion character varying
);

CREATE TABLE IF NOT EXISTS hc.antecedentes (
    id character varying(50) NOT NULL,
    historia_id character varying(50) NOT NULL,
    tipo character varying(50) NOT NULL,
    descripcion text,
    fecha date,
    created_at timestamp with time zone DEFAULT now(),
    CONSTRAINT antecedentes_tipo_check CHECK (((tipo)::text = ANY (ARRAY[('PATOLÓGICOS'::character varying)::text, ('QUIRÚRGICOS'::character varying)::text, ('MEDICAMENTOS EN USO'::character varying)::text, ('FAMILIARES'::character varying)::text, ('ALERGIAS A MEDICAMENTOS'::character varying)::text, ('OCULARES'::character varying)::text, ('ACTIVIDAD FÍSICA'::character varying)::text, ('TÓXICOS'::character varying)::text, ('OTRO'::character varying)::text, ('AYUDAS DIAGNÓSTICAS'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS hc.catalogo_cie10 (
    id uuid NOT NULL,
    codigo character varying(10) NOT NULL,
    descripcion character varying(400) NOT NULL,
    categoria character varying(5),
    activo boolean DEFAULT true
);

CREATE TABLE IF NOT EXISTS hc.catalogo_medicamentos (
    id uuid NOT NULL,
    nombre_generico character varying(300) NOT NULL,
    nombre_comercial character varying(300),
    registro_invima character varying(100),
    laboratorio character varying(200),
    activo boolean DEFAULT true
);

CREATE TABLE IF NOT EXISTS hc.controles_consulta (
    id character varying(50) NOT NULL,
    historia_id character varying(50) NOT NULL,
    admision_id uuid,
    medico_id uuid,
    sede_id uuid,
    numero_control integer DEFAULT 1 NOT NULL,
    estado character varying(10) DEFAULT 'GRABADA'::character varying NOT NULL,
    fecha_consulta timestamp with time zone DEFAULT now(),
    motivo_consulta text,
    enfermedad_actual text,
    finalidad_consulta character varying(5),
    causa_externa character varying(5),
    codigo_dx character varying(10),
    descripcion_dx character varying(400),
    tipo_dx character varying(50),
    diagnostico_clinico character varying(200),
    evaluacion_paraclinicos text,
    analisis_plan text,
    medico_tipo_doc character varying(5),
    medico_num_doc character varying(50),
    medico_nombre character varying(150),
    medico_registro character varying(255),
    medico_especialidad character varying(100),
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT controles_consulta_estado_check CHECK (((estado)::text = ANY (ARRAY[('GRABADA'::character varying)::text, ('CERRADA'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS hc.examen_fisico (
    id character varying(50) NOT NULL,
    control_id character varying(50) NOT NULL,
    cabeza text DEFAULT 'Normal'::text,
    ojos text DEFAULT 'Normal'::text,
    oidos text DEFAULT 'Normal'::text,
    nariz text DEFAULT 'Normal'::text,
    boca text DEFAULT 'Normal'::text,
    garganta text DEFAULT 'Normal'::text,
    cuello text DEFAULT 'Normal'::text,
    torax text DEFAULT 'Normal'::text,
    corazon text DEFAULT 'Normal'::text,
    pulmon text DEFAULT 'Normal'::text,
    abdomen text DEFAULT 'Normal'::text,
    pelvis text DEFAULT 'Normal'::text,
    tacto_rectal text DEFAULT 'Normal'::text,
    genitourinario_examen text DEFAULT 'Normal'::text,
    extremidades_superiores text DEFAULT 'Normal'::text,
    extremidades_inferiores text,
    espalda text DEFAULT 'Normal'::text,
    piel_examen text DEFAULT 'Normal'::text,
    endocrino_examen text DEFAULT 'Normal'::text,
    sistema_nervioso text DEFAULT 'Normal'::text,
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS hc.formulas_medicas (
    id character varying(50) NOT NULL,
    control_id character varying(50) NOT NULL,
    medicamento_id uuid,
    codigo character varying(20),
    descripcion text NOT NULL,
    dosis character varying(100),
    cantidad character varying(20),
    observacion text,
    fecha_hora timestamp with time zone,
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS hc.historias_clinicas (
    id character varying(50) NOT NULL,
    numero_hc character varying(30) NOT NULL,
    estado character varying(10) DEFAULT 'GRABADA'::character varying NOT NULL,
    motivo_consulta_inicial text,
    fecha_apertura timestamp with time zone DEFAULT now(),
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    paciente_id uuid,
    medico_id uuid,
    sede_id integer,
    CONSTRAINT historias_clinicas_estado_check CHECK (((estado)::text = ANY (ARRAY[('GRABADA'::character varying)::text, ('CERRADA'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS hc.indicaciones (
    id character varying(50) NOT NULL,
    control_id character varying(50) NOT NULL,
    descripcion text NOT NULL,
    fecha timestamp with time zone,
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS hc.juntas_medicas (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    control_id character varying NOT NULL,
    medico_id uuid NOT NULL,
    especialidad_id uuid,
    descripcion text,
    fecha date NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS hc.medico_especialidades (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    medico_id uuid NOT NULL,
    especialidad character varying(150) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS hc.medicos (
    nombre character varying(100) NOT NULL,
    apellido character varying(100) NOT NULL,
    tipo_doc character varying(10) NOT NULL,
    num_doc character varying(30) NOT NULL,
    registro character varying(255) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL
);

CREATE TABLE IF NOT EXISTS hc.revision_sistemas (
    id character varying(50) NOT NULL,
    control_id character varying(50) NOT NULL,
    respiratorio text DEFAULT 'Normal'::text,
    organos_sentidos text DEFAULT 'Normal'::text,
    cardiovascular text DEFAULT 'Normal'::text,
    gastrointestinal text DEFAULT 'Normal'::text,
    genitourinario text DEFAULT 'Normal'::text,
    neurologico text DEFAULT 'Normal'::text,
    piel_anexos text DEFAULT 'Normal'::text,
    osteomuscular text DEFAULT 'Normal'::text,
    endocrino text DEFAULT 'Normal'::text,
    psicosocial text DEFAULT 'Normal'::text,
    linfatico text DEFAULT 'Normal'::text,
    otro_sistema text DEFAULT 'Normal'::text,
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS hc.signos_vitales (
    id character varying(50) NOT NULL,
    control_id character varying(50) NOT NULL,
    presion_arterial character varying(20),
    pulso character varying(20),
    frecuencia_respiratoria character varying(20),
    temperatura character varying(10),
    peso character varying(10),
    talla character varying(10),
    saturacion character varying(10),
    escala_dolor character varying(5),
    imc character varying(10),
    pam character varying(10),
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS hc.solicitudes (
    id character varying(50) NOT NULL,
    control_id character varying(50) NOT NULL,
    codigo character varying(20),
    descripcion text NOT NULL,
    cantidad character varying(10),
    observacion text,
    fecha_hora timestamp with time zone,
    created_at timestamp with time zone DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_audit_log_pkey') THEN
        ALTER TABLE ONLY appointments.appointment_audit_log
    ADD CONSTRAINT appointment_audit_log_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_participants_pkey') THEN
        ALTER TABLE ONLY appointments.appointment_participants
    ADD CONSTRAINT appointment_participants_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_resource_allocations_pkey') THEN
        ALTER TABLE ONLY appointments.appointment_resource_allocations
    ADD CONSTRAINT appointment_resource_allocations_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_state_history_pkey') THEN
        ALTER TABLE ONLY appointments.appointment_state_history
    ADD CONSTRAINT appointment_state_history_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointments_pkey') THEN
        ALTER TABLE ONLY appointments.appointments
    ADD CONSTRAINT appointments_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'domain_events_pkey') THEN
        ALTER TABLE ONLY appointments.domain_events
    ADD CONSTRAINT domain_events_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'facility_operating_hours_pkey') THEN
        ALTER TABLE ONLY appointments.facility_operating_hours
    ADD CONSTRAINT facility_operating_hours_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'facility_resources_pkey') THEN
        ALTER TABLE ONLY appointments.facility_resources
    ADD CONSTRAINT facility_resources_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'n8n_idempotency_keys_pkey') THEN
        ALTER TABLE ONLY appointments.n8n_idempotency_keys
    ADD CONSTRAINT n8n_idempotency_keys_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'notifications_pkey') THEN
        ALTER TABLE ONLY appointments.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pqrs_pkey') THEN
        ALTER TABLE ONLY appointments.pqrs
    ADD CONSTRAINT pqrs_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'schedule_blocks_pkey') THEN
        ALTER TABLE ONLY appointments.schedule_blocks
    ADD CONSTRAINT schedule_blocks_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'schedule_plan_blocks_pkey') THEN
        ALTER TABLE ONLY appointments.schedule_plan_blocks
    ADD CONSTRAINT schedule_plan_blocks_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'schedule_plan_slots_pkey') THEN
        ALTER TABLE ONLY appointments.schedule_plan_slots
    ADD CONSTRAINT schedule_plan_slots_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'schedule_plans_pkey') THEN
        ALTER TABLE ONLY appointments.schedule_plans
    ADD CONSTRAINT schedule_plans_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'schedules_pkey') THEN
        ALTER TABLE ONLY appointments.schedules
    ADD CONSTRAINT schedules_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'sede_code_aliases_pkey') THEN
        ALTER TABLE ONLY appointments.sede_code_aliases
    ADD CONSTRAINT sede_code_aliases_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'specialist_metadata_pkey') THEN
        ALTER TABLE ONLY appointments.specialist_metadata
    ADD CONSTRAINT specialist_metadata_pkey PRIMARY KEY (profile_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uk_rs82qn6miu0vu0bw00us88dc4') THEN
        ALTER TABLE ONLY appointments.pqrs
    ADD CONSTRAINT uk_rs82qn6miu0vu0bw00us88dc4 UNIQUE (radicado);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uq_appointment_participants_doctor') THEN
        ALTER TABLE ONLY appointments.appointment_participants
    ADD CONSTRAINT uq_appointment_participants_doctor UNIQUE (appointment_id, doctor_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uq_appointment_participants_order') THEN
        ALTER TABLE ONLY appointments.appointment_participants
    ADD CONSTRAINT uq_appointment_participants_order UNIQUE (appointment_id, participant_order);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uq_appointment_resource_allocations_appointment') THEN
        ALTER TABLE ONLY appointments.appointment_resource_allocations
    ADD CONSTRAINT uq_appointment_resource_allocations_appointment UNIQUE (appointment_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uq_facility_operating_hours_sede_day') THEN
        ALTER TABLE ONLY appointments.facility_operating_hours
    ADD CONSTRAINT uq_facility_operating_hours_sede_day UNIQUE (sede_id, day_of_week);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uq_facility_resources_sede_code') THEN
        ALTER TABLE ONLY appointments.facility_resources
    ADD CONSTRAINT uq_facility_resources_sede_code UNIQUE (sede_id, code);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uq_n8n_idempotency_scope_key') THEN
        ALTER TABLE ONLY appointments.n8n_idempotency_keys
    ADD CONSTRAINT uq_n8n_idempotency_scope_key UNIQUE (scope, idempotency_key);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uq_schedule_plan_version') THEN
        ALTER TABLE ONLY appointments.schedule_plans
    ADD CONSTRAINT uq_schedule_plan_version UNIQUE (specialist_id, start_date, end_date, version_number);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'uq_sede_code_aliases_code') THEN
        ALTER TABLE ONLY appointments.sede_code_aliases
    ADD CONSTRAINT uq_sede_code_aliases_code UNIQUE (alias_code);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'admisiones_pkey') THEN
        ALTER TABLE ONLY core.admisiones
    ADD CONSTRAINT admisiones_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_admissions_appointment_id_key') THEN
        ALTER TABLE ONLY core.appointment_admissions
    ADD CONSTRAINT appointment_admissions_appointment_id_key UNIQUE (appointment_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_admissions_pkey') THEN
        ALTER TABLE ONLY core.appointment_admissions
    ADD CONSTRAINT appointment_admissions_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'archivos_pkey') THEN
        ALTER TABLE ONLY core.archivos
    ADD CONSTRAINT archivos_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'autorizaciones_pkey') THEN
        ALTER TABLE ONLY core.autorizaciones
    ADD CONSTRAINT autorizaciones_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'coberturas_salud_pkey') THEN
        ALTER TABLE ONLY core.coberturas_salud
    ADD CONSTRAINT coberturas_salud_pkey PRIMARY KEY (cod);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'conceptos_recaudo_pkey') THEN
        ALTER TABLE ONLY core.conceptos_recaudo
    ADD CONSTRAINT conceptos_recaudo_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'contacto_email_key') THEN
        ALTER TABLE ONLY core.contacto
    ADD CONSTRAINT contacto_email_key UNIQUE (email);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'contacto_pkey') THEN
        ALTER TABLE ONLY core.contacto
    ADD CONSTRAINT contacto_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'contacto_telefono_key') THEN
        ALTER TABLE ONLY core.contacto
    ADD CONSTRAINT contacto_telefono_key UNIQUE (telefono);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'contactos_paciente_pkey') THEN
        ALTER TABLE ONLY core.contactos_paciente
    ADD CONSTRAINT contactos_paciente_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_id_autorizacion_key') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_id_autorizacion_key UNIQUE (id_autorizacion);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_id_recaudo_key') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_id_recaudo_key UNIQUE (id_recaudo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_pkey') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'departamento_pkey') THEN
        ALTER TABLE ONLY core.departamento
    ADD CONSTRAINT departamento_pkey PRIMARY KEY (codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'direccion_pkey') THEN
        ALTER TABLE ONLY core.direccion
    ADD CONSTRAINT direccion_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_nit_tipo_fact_key') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_nit_tipo_fact_key UNIQUE (num_identificacion, tipo_facturacion);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_numero_poliza_key') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_numero_poliza_key UNIQUE (numero_contrato);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_pkey') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'escolaridades_pkey') THEN
        ALTER TABLE ONLY core.escolaridades
    ADD CONSTRAINT escolaridades_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'estado_civil_pkey') THEN
        ALTER TABLE ONLY core.estados_civil
    ADD CONSTRAINT estado_civil_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'estados_cuenta_medica_pkey') THEN
        ALTER TABLE ONLY core.estados_cuenta_medica
    ADD CONSTRAINT estados_cuenta_medica_pkey PRIMARY KEY (codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'generos_pkey') THEN
        ALTER TABLE ONLY core.generos
    ADD CONSTRAINT generos_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'grupos_sanguineos_pkey') THEN
        ALTER TABLE ONLY core.grupos_sanguineos
    ADD CONSTRAINT grupos_sanguineos_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'item_cuenta_medica_pkey') THEN
        ALTER TABLE ONLY core.item_cuenta_medica
    ADD CONSTRAINT item_cuenta_medica_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'municipio_pkey') THEN
        ALTER TABLE ONLY core.municipio
    ADD CONSTRAINT municipio_pkey PRIMARY KEY (cod);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'ocupaciones_pkey') THEN
        ALTER TABLE ONLY core.ocupaciones
    ADD CONSTRAINT ocupaciones_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_audit_log_pkey') THEN
        ALTER TABLE ONLY core.pacientes_audit_log
    ADD CONSTRAINT pacientes_audit_log_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_num_tipo_identificacion_constraint') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_num_tipo_identificacion_constraint UNIQUE (num_identificacion, cod_tipo_identificacion);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_pkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'paises_pkey') THEN
        ALTER TABLE ONLY core.paises
    ADD CONSTRAINT paises_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'parentescos_codigo_key') THEN
        ALTER TABLE ONLY core.parentescos
    ADD CONSTRAINT parentescos_codigo_key UNIQUE (codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'parentescos_nombre_unique') THEN
        ALTER TABLE ONLY core.parentescos
    ADD CONSTRAINT parentescos_nombre_unique UNIQUE (nombre);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'parentescos_pkey') THEN
        ALTER TABLE ONLY core.parentescos
    ADD CONSTRAINT parentescos_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'profiles_pkey') THEN
        ALTER TABLE ONLY core.profiles
    ADD CONSTRAINT profiles_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'refresh_tokens_pkey') THEN
        ALTER TABLE ONLY core.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'roles_nombre_key') THEN
        ALTER TABLE ONLY core.roles
    ADD CONSTRAINT roles_nombre_key UNIQUE (nombre);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'roles_pkey') THEN
        ALTER TABLE ONLY core.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'sede_pkey') THEN
        ALTER TABLE ONLY core.sede
    ADD CONSTRAINT sede_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'tipo_identificacion_pkey') THEN
        ALTER TABLE ONLY core.tipo_identificacion
    ADD CONSTRAINT tipo_identificacion_pkey PRIMARY KEY (codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'tipo_usuario_pkey') THEN
        ALTER TABLE ONLY core.tipo_usuario
    ADD CONSTRAINT tipo_usuario_pkey PRIMARY KEY (cod);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'tipos_facturacion_pkey') THEN
        ALTER TABLE ONLY core.tipos_facturacion
    ADD CONSTRAINT tipos_facturacion_pkey PRIMARY KEY (codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'tipos_recaudo_pkey') THEN
        ALTER TABLE ONLY core.tipo_recaudo
    ADD CONSTRAINT tipos_recaudo_pkey PRIMARY KEY (codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'zonas_territoriales_pkey') THEN
        ALTER TABLE ONLY core.zonas_territoriales
    ADD CONSTRAINT zonas_territoriales_pkey PRIMARY KEY (cod);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'antecedentes_pkey') THEN
        ALTER TABLE ONLY hc.antecedentes
    ADD CONSTRAINT antecedentes_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'catalogo_cie10_codigo_key') THEN
        ALTER TABLE ONLY hc.catalogo_cie10
    ADD CONSTRAINT catalogo_cie10_codigo_key UNIQUE (codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'catalogo_cie10_pkey') THEN
        ALTER TABLE ONLY hc.catalogo_cie10
    ADD CONSTRAINT catalogo_cie10_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'catalogo_medicamentos_pkey') THEN
        ALTER TABLE ONLY hc.catalogo_medicamentos
    ADD CONSTRAINT catalogo_medicamentos_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'controles_consulta_pkey') THEN
        ALTER TABLE ONLY hc.controles_consulta
    ADD CONSTRAINT controles_consulta_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'examen_fisico_control_id_key') THEN
        ALTER TABLE ONLY hc.examen_fisico
    ADD CONSTRAINT examen_fisico_control_id_key UNIQUE (control_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'examen_fisico_pkey') THEN
        ALTER TABLE ONLY hc.examen_fisico
    ADD CONSTRAINT examen_fisico_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'formulas_medicas_pkey') THEN
        ALTER TABLE ONLY hc.formulas_medicas
    ADD CONSTRAINT formulas_medicas_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'historias_clinicas_pkey') THEN
        ALTER TABLE ONLY hc.historias_clinicas
    ADD CONSTRAINT historias_clinicas_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'indicaciones_pkey') THEN
        ALTER TABLE ONLY hc.indicaciones
    ADD CONSTRAINT indicaciones_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'juntas_medicas_pkey') THEN
        ALTER TABLE ONLY hc.juntas_medicas
    ADD CONSTRAINT juntas_medicas_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'medico_especialidades_pkey') THEN
        ALTER TABLE ONLY hc.medico_especialidades
    ADD CONSTRAINT medico_especialidades_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'medicos_num_doc_key') THEN
        ALTER TABLE ONLY hc.medicos
    ADD CONSTRAINT medicos_num_doc_key UNIQUE (num_doc);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'medicos_pkey') THEN
        ALTER TABLE ONLY hc.medicos
    ADD CONSTRAINT medicos_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'revision_sistemas_control_id_key') THEN
        ALTER TABLE ONLY hc.revision_sistemas
    ADD CONSTRAINT revision_sistemas_control_id_key UNIQUE (control_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'revision_sistemas_pkey') THEN
        ALTER TABLE ONLY hc.revision_sistemas
    ADD CONSTRAINT revision_sistemas_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'signos_vitales_control_id_key') THEN
        ALTER TABLE ONLY hc.signos_vitales
    ADD CONSTRAINT signos_vitales_control_id_key UNIQUE (control_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'signos_vitales_pkey') THEN
        ALTER TABLE ONLY hc.signos_vitales
    ADD CONSTRAINT signos_vitales_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'solicitudes_pkey') THEN
        ALTER TABLE ONLY hc.solicitudes
    ADD CONSTRAINT solicitudes_pkey PRIMARY KEY (id);
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_appointment_audit_log_appointment ON appointments.appointment_audit_log USING btree (appointment_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_appointment_audit_log_performed_at ON appointments.appointment_audit_log USING btree (performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_appointment_participants_appointment ON appointments.appointment_participants USING btree (appointment_id);

CREATE INDEX IF NOT EXISTS idx_appointment_participants_doctor ON appointments.appointment_participants USING btree (doctor_id, appointment_id);

CREATE INDEX IF NOT EXISTS idx_appointment_participants_doctor_core ON appointments.appointment_participants USING btree (doctor_id);

CREATE INDEX IF NOT EXISTS idx_appointment_participants_doctor_date ON appointments.appointment_participants USING btree (doctor_id, appointment_id);

CREATE INDEX IF NOT EXISTS idx_appointment_resource_allocations_lookup ON appointments.appointment_resource_allocations USING btree (sede_id, resource_type, appointment_date, start_time, end_time);

CREATE INDEX IF NOT EXISTS idx_appointment_state_history_appointment ON appointments.appointment_state_history USING btree (appointment_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_appointment_state_history_changed_at ON appointments.appointment_state_history USING btree (changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_appointment_state_history_user ON appointments.appointment_state_history USING btree (changed_by, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_appointments_booking_channel ON appointments.appointments USING btree (booking_channel, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_appointments_date_status ON appointments.appointments USING btree (appointment_date, status);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments.appointments USING btree (patient_id);

CREATE INDEX IF NOT EXISTS idx_appointments_patient_core ON appointments.appointments USING btree (patient_id);

CREATE INDEX IF NOT EXISTS idx_appointments_patient_date_status ON appointments.appointments USING btree (patient_id, appointment_date DESC, status);

CREATE INDEX IF NOT EXISTS idx_appointments_sede_date_status ON appointments.appointments USING btree (sede_id, appointment_date, status);

CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments.appointments USING btree (status);

CREATE INDEX IF NOT EXISTS idx_appointments_type_date ON appointments.appointments USING btree (appointment_type, appointment_date DESC) WHERE ((status)::text <> 'CANCELLED'::text);

CREATE INDEX IF NOT EXISTS idx_domain_events_aggregate ON appointments.domain_events USING btree (aggregate_id, occurred_on);

CREATE INDEX IF NOT EXISTS idx_domain_events_type ON appointments.domain_events USING btree (event_type);

CREATE INDEX IF NOT EXISTS idx_domain_events_unpublished ON appointments.domain_events USING btree (published) WHERE (published = false);

CREATE INDEX IF NOT EXISTS idx_facility_operating_hours_sede ON appointments.facility_operating_hours USING btree (sede_id);

CREATE INDEX IF NOT EXISTS idx_facility_resources_sede_type_active ON appointments.facility_resources USING btree (sede_id, resource_type, is_active);

CREATE INDEX IF NOT EXISTS idx_n8n_idempotency_appointment ON appointments.n8n_idempotency_keys USING btree (appointment_id);

CREATE INDEX IF NOT EXISTS idx_notifications_entity ON appointments.notifications USING btree (entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_notifications_pending ON appointments.notifications USING btree (status, created_at) WHERE ((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('FAILED'::character varying)::text]));

CREATE INDEX IF NOT EXISTS idx_notifications_status_created_at ON appointments.notifications USING btree (status, created_at);

CREATE INDEX IF NOT EXISTS idx_pqrs_cedula ON appointments.pqrs USING btree (cedula);

CREATE INDEX IF NOT EXISTS idx_pqrs_created_at ON appointments.pqrs USING btree (created_at);

CREATE INDEX IF NOT EXISTS idx_pqrs_radicado ON appointments.pqrs USING btree (radicado);

CREATE INDEX IF NOT EXISTS idx_pqrs_status ON appointments.pqrs USING btree (status);

CREATE INDEX IF NOT EXISTS idx_resource_alloc_session_key ON appointments.appointment_resource_allocations USING btree (capacity_session_key) WHERE (released_at IS NULL);

CREATE INDEX IF NOT EXISTS idx_schedule_blocks_created_by ON appointments.schedule_blocks USING btree (created_by);

CREATE INDEX IF NOT EXISTS idx_schedule_blocks_schedule_date ON appointments.schedule_blocks USING btree (schedule_id, block_date);

CREATE INDEX IF NOT EXISTS idx_schedule_plan_blocks_created_by ON appointments.schedule_plan_blocks USING btree (created_by);

CREATE INDEX IF NOT EXISTS idx_schedule_plan_blocks_plan_date ON appointments.schedule_plan_blocks USING btree (schedule_plan_id, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_schedule_plan_slots_plan ON appointments.schedule_plan_slots USING btree (schedule_plan_id, day_of_week);

CREATE INDEX IF NOT EXISTS idx_schedule_plans_sede_dates ON appointments.schedule_plans USING btree (sede_id, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_schedule_plans_specialist_core ON appointments.schedule_plans USING btree (specialist_id);

CREATE INDEX IF NOT EXISTS idx_schedule_plans_specialist_dates ON appointments.schedule_plans USING btree (specialist_id, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_schedules_doctor_core ON appointments.schedules USING btree (doctor_id);

CREATE INDEX IF NOT EXISTS idx_schedules_doctor_day ON appointments.schedules USING btree (doctor_id, day_of_week);

CREATE INDEX IF NOT EXISTS idx_schedules_doctor_sede_day ON appointments.schedules USING btree (doctor_id, sede_id, day_of_week);

CREATE INDEX IF NOT EXISTS idx_schedules_sede ON appointments.schedules USING btree (sede_id);

CREATE INDEX IF NOT EXISTS idx_sede_code_aliases_sede ON appointments.sede_code_aliases USING btree (sede_id);

CREATE INDEX IF NOT EXISTS idx_specialist_metadata_active ON appointments.specialist_metadata USING btree (is_active);

CREATE INDEX IF NOT EXISTS idx_specialist_metadata_synced ON appointments.specialist_metadata USING btree (synced_from_hc);

CREATE UNIQUE INDEX IF NOT EXISTS uq_schedule_plan_active_period ON appointments.schedule_plans USING btree (specialist_id, start_date, end_date) WHERE (is_active_version = true);

CREATE UNIQUE INDEX IF NOT EXISTS cuentas_medicas_unica_abierta ON core.cuentas_medicas USING btree (id_paciente, id_entidad, id_autorizacion);

CREATE INDEX IF NOT EXISTS idx_appointment_admissions_admission ON core.appointment_admissions USING btree (admission_id);

CREATE INDEX IF NOT EXISTS idx_pacientes_audit_paciente ON core.pacientes_audit_log USING btree (paciente_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_profiles_medico_id ON core.profiles USING btree (medico_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON core.refresh_tokens USING btree (token_hash);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_profile ON core.refresh_tokens USING btree (profile_id);

CREATE INDEX IF NOT EXISTS idx_antecedentes_historia ON hc.antecedentes USING btree (historia_id);

CREATE INDEX IF NOT EXISTS idx_controles_admision ON hc.controles_consulta USING btree (admision_id);

CREATE INDEX IF NOT EXISTS idx_controles_fecha ON hc.controles_consulta USING btree (fecha_consulta DESC);

CREATE INDEX IF NOT EXISTS idx_controles_historia ON hc.controles_consulta USING btree (historia_id);

CREATE INDEX IF NOT EXISTS idx_formulas_control ON hc.formulas_medicas USING btree (control_id);

CREATE INDEX IF NOT EXISTS idx_hc_numero ON hc.historias_clinicas USING btree (numero_hc);

CREATE INDEX IF NOT EXISTS idx_medicos_num_doc ON hc.medicos USING btree (num_doc);

CREATE INDEX IF NOT EXISTS idx_solicitudes_control ON hc.solicitudes USING btree (control_id);

DROP TRIGGER IF EXISTS trg_appointments_updated_at ON appointments.appointments;
CREATE TRIGGER trg_appointments_updated_at BEFORE UPDATE ON appointments.appointments FOR EACH ROW EXECUTE FUNCTION appointments.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_facility_operating_hours_updated_at ON appointments.facility_operating_hours;
CREATE TRIGGER trg_facility_operating_hours_updated_at BEFORE UPDATE ON appointments.facility_operating_hours FOR EACH ROW EXECUTE FUNCTION appointments.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_facility_resources_updated_at ON appointments.facility_resources;
CREATE TRIGGER trg_facility_resources_updated_at BEFORE UPDATE ON appointments.facility_resources FOR EACH ROW EXECUTE FUNCTION appointments.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_log_status_change ON appointments.appointments;
CREATE TRIGGER trg_log_status_change AFTER UPDATE ON appointments.appointments FOR EACH ROW EXECUTE FUNCTION appointments.log_status_change();

DROP TRIGGER IF EXISTS trg_schedule_plans_updated_at ON appointments.schedule_plans;
CREATE TRIGGER trg_schedule_plans_updated_at BEFORE UPDATE ON appointments.schedule_plans FOR EACH ROW EXECUTE FUNCTION appointments.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_schedules_updated_at ON appointments.schedules;
CREATE TRIGGER trg_schedules_updated_at BEFORE UPDATE ON appointments.schedules FOR EACH ROW EXECUTE FUNCTION appointments.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_specialist_metadata_updated_at ON appointments.specialist_metadata;
CREATE TRIGGER trg_specialist_metadata_updated_at BEFORE UPDATE ON appointments.specialist_metadata FOR EACH ROW EXECUTE FUNCTION appointments.update_updated_at_column();

DROP TRIGGER IF EXISTS trg_validate_facility ON appointments.appointments;
CREATE TRIGGER trg_validate_facility BEFORE INSERT OR UPDATE ON appointments.appointments FOR EACH ROW EXECUTE FUNCTION appointments.validate_appointment_facility();

DROP TRIGGER IF EXISTS trg_controles_updated_at ON hc.controles_consulta;
CREATE TRIGGER trg_controles_updated_at BEFORE UPDATE ON hc.controles_consulta FOR EACH ROW EXECUTE FUNCTION hc.actualizar_updated_at();

DROP TRIGGER IF EXISTS trg_historias_updated_at ON hc.historias_clinicas;
CREATE TRIGGER trg_historias_updated_at BEFORE UPDATE ON hc.historias_clinicas FOR EACH ROW EXECUTE FUNCTION hc.actualizar_updated_at();

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_audit_log_appointment_id_fkey') THEN
        ALTER TABLE ONLY appointments.appointment_audit_log
    ADD CONSTRAINT appointment_audit_log_appointment_id_fkey FOREIGN KEY (appointment_id) REFERENCES appointments.appointments(id) ON DELETE CASCADE;
    END IF;
END;
$$;

ALTER TABLE ONLY appointments.appointment_audit_log
    ADD CONSTRAINT appointment_audit_log_performed_by_fkey FOREIGN KEY (performed_by) REFERENCES core.profiles(id) ON DELETE SET NULL;

ALTER TABLE ONLY appointments.appointment_state_history
    ADD CONSTRAINT appointment_state_history_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES core.profiles(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointments_patient_id_fkey') THEN
        ALTER TABLE ONLY appointments.appointments
    ADD CONSTRAINT appointments_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES core.pacientes(id) ON DELETE RESTRICT;
    END IF;
END;
$$;

ALTER TABLE ONLY appointments.appointments
    ADD CONSTRAINT appointments_schedule_id_fkey FOREIGN KEY (schedule_id) REFERENCES appointments.schedules(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_appointment_resource_allocations_sede') THEN
        ALTER TABLE ONLY appointments.appointment_resource_allocations
    ADD CONSTRAINT fk_appointment_resource_allocations_sede FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE RESTRICT;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_appointments_patient') THEN
        ALTER TABLE ONLY appointments.appointments
    ADD CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES core.pacientes(id) ON UPDATE CASCADE ON DELETE RESTRICT;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_appointments_sede') THEN
        ALTER TABLE ONLY appointments.appointments
    ADD CONSTRAINT fk_appointments_sede FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE RESTRICT;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_facility_operating_hours_sede') THEN
        ALTER TABLE ONLY appointments.facility_operating_hours
    ADD CONSTRAINT fk_facility_operating_hours_sede FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_facility_resources_sede') THEN
        ALTER TABLE ONLY appointments.facility_resources
    ADD CONSTRAINT fk_facility_resources_sede FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END;
$$;

ALTER TABLE ONLY appointments.appointment_resource_allocations
    ADD CONSTRAINT fk_resource_alloc_facility_resource FOREIGN KEY (facility_resource_id) REFERENCES appointments.facility_resources(id) ON DELETE SET NULL;

ALTER TABLE ONLY appointments.schedule_blocks
    ADD CONSTRAINT fk_schedule_blocks_created_by FOREIGN KEY (created_by) REFERENCES core.profiles(id) ON DELETE SET NULL;

ALTER TABLE ONLY appointments.schedule_plan_blocks
    ADD CONSTRAINT fk_schedule_plan_blocks_created_by FOREIGN KEY (created_by) REFERENCES core.profiles(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_schedule_plans_sede') THEN
        ALTER TABLE ONLY appointments.schedule_plans
    ADD CONSTRAINT fk_schedule_plans_sede FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON DELETE RESTRICT;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_schedules_sede') THEN
        ALTER TABLE ONLY appointments.schedules
    ADD CONSTRAINT fk_schedules_sede FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON UPDATE CASCADE ON DELETE RESTRICT;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_sede_code_aliases_sede') THEN
        ALTER TABLE ONLY appointments.sede_code_aliases
    ADD CONSTRAINT fk_sede_code_aliases_sede FOREIGN KEY (sede_id) REFERENCES core.sede(id) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'schedule_blocks_schedule_id_fkey') THEN
        ALTER TABLE ONLY appointments.schedule_blocks
    ADD CONSTRAINT schedule_blocks_schedule_id_fkey FOREIGN KEY (schedule_id) REFERENCES appointments.schedules(id) ON DELETE CASCADE;
    END IF;
END;
$$;

ALTER TABLE ONLY appointments.schedule_plan_blocks
    ADD CONSTRAINT schedule_plan_blocks_created_by_fkey FOREIGN KEY (created_by) REFERENCES core.profiles(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'schedule_plan_blocks_schedule_plan_id_fkey') THEN
        ALTER TABLE ONLY appointments.schedule_plan_blocks
    ADD CONSTRAINT schedule_plan_blocks_schedule_plan_id_fkey FOREIGN KEY (schedule_plan_id) REFERENCES appointments.schedule_plans(id) ON DELETE CASCADE;
    END IF;
END;
$$;

ALTER TABLE ONLY appointments.schedule_plan_slots
    ADD CONSTRAINT schedule_plan_slots_consultorio_id_fkey FOREIGN KEY (consultorio_id) REFERENCES appointments.facility_resources(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'schedule_plan_slots_schedule_plan_id_fkey') THEN
        ALTER TABLE ONLY appointments.schedule_plan_slots
    ADD CONSTRAINT schedule_plan_slots_schedule_plan_id_fkey FOREIGN KEY (schedule_plan_id) REFERENCES appointments.schedule_plans(id) ON DELETE CASCADE;
    END IF;
END;
$$;

ALTER TABLE ONLY appointments.schedules
    ADD CONSTRAINT schedules_consultorio_id_fkey FOREIGN KEY (consultorio_id) REFERENCES appointments.facility_resources(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'specialist_metadata_profile_id_fkey') THEN
        ALTER TABLE ONLY appointments.specialist_metadata
    ADD CONSTRAINT specialist_metadata_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES core.profiles(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'admisiones_admitido_por_fkey') THEN
        ALTER TABLE ONLY core.admisiones
    ADD CONSTRAINT admisiones_admitido_por_fkey FOREIGN KEY (admitido_por) REFERENCES core.profiles(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'admisiones_autorizacion_fkey') THEN
        ALTER TABLE ONLY core.admisiones
    ADD CONSTRAINT admisiones_autorizacion_fkey FOREIGN KEY (autorizacion) REFERENCES core.autorizaciones(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'admisiones_firma_asistencia_archivo_id_fkey') THEN
        ALTER TABLE ONLY core.admisiones
    ADD CONSTRAINT admisiones_firma_asistencia_archivo_id_fkey FOREIGN KEY (firma_asistencia_archivo_id) REFERENCES core.archivos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'admisiones_firma_asistencia_biometria_id_fkey') THEN
        ALTER TABLE ONLY core.admisiones
    ADD CONSTRAINT admisiones_firma_asistencia_biometria_id_fkey FOREIGN KEY (firma_asistencia_biometria_id) REFERENCES core.archivos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'admisiones_id_contacto_paciente_fkey') THEN
        ALTER TABLE ONLY core.admisiones
    ADD CONSTRAINT admisiones_id_contacto_paciente_fkey FOREIGN KEY (id_contacto_paciente) REFERENCES core.contactos_paciente(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'admisiones_id_entidad_fkey') THEN
        ALTER TABLE ONLY core.admisiones
    ADD CONSTRAINT admisiones_id_entidad_fkey FOREIGN KEY (id_entidad) REFERENCES core.entidades(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'admisiones_id_paciente_fkey') THEN
        ALTER TABLE ONLY core.admisiones
    ADD CONSTRAINT admisiones_id_paciente_fkey FOREIGN KEY (id_paciente) REFERENCES core.pacientes(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_admissions_admission_id_fkey') THEN
        ALTER TABLE ONLY core.appointment_admissions
    ADD CONSTRAINT appointment_admissions_admission_id_fkey FOREIGN KEY (admission_id) REFERENCES core.admisiones(id) ON DELETE RESTRICT;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'appointment_admissions_appointment_id_fkey') THEN
        ALTER TABLE ONLY core.appointment_admissions
    ADD CONSTRAINT appointment_admissions_appointment_id_fkey FOREIGN KEY (appointment_id) REFERENCES appointments.appointments(id) ON DELETE CASCADE;
    END IF;
END;
$$;

ALTER TABLE ONLY core.appointment_admissions
    ADD CONSTRAINT appointment_admissions_linked_by_fkey FOREIGN KEY (linked_by) REFERENCES core.profiles(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'autorizaciones_id_entidad_fkey') THEN
        ALTER TABLE ONLY core.autorizaciones
    ADD CONSTRAINT autorizaciones_id_entidad_fkey FOREIGN KEY (id_entidad) REFERENCES core.entidades(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'autorizaciones_servicio_autorizado_fkey') THEN
        ALTER TABLE ONLY core.autorizaciones
    ADD CONSTRAINT autorizaciones_servicio_autorizado_fkey FOREIGN KEY (servicio_autorizado) REFERENCES servicios_salud.procedimientos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'conceptos_recaudo_cod_tipo_recaudo_fkey') THEN
        ALTER TABLE ONLY core.conceptos_recaudo
    ADD CONSTRAINT conceptos_recaudo_cod_tipo_recaudo_fkey FOREIGN KEY (cod_tipo_recaudo) REFERENCES core.tipo_recaudo(codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'contactos_paciente_id_paciente_fkey') THEN
        ALTER TABLE ONLY core.contactos_paciente
    ADD CONSTRAINT contactos_paciente_id_paciente_fkey FOREIGN KEY (id_paciente) REFERENCES core.pacientes(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'contactos_paciente_id_parentesco_fkey') THEN
        ALTER TABLE ONLY core.contactos_paciente
    ADD CONSTRAINT contactos_paciente_id_parentesco_fkey FOREIGN KEY (id_parentesco) REFERENCES core.parentescos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_abierta_por_fkey') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_abierta_por_fkey FOREIGN KEY (abierta_por) REFERENCES core.profiles(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_consentimiento_biometria_id_fkey') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_consentimiento_biometria_id_fkey FOREIGN KEY (consentimiento_biometria_id) REFERENCES core.archivos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_id_autorizacion_fkey') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_id_autorizacion_fkey FOREIGN KEY (id_autorizacion) REFERENCES core.autorizaciones(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_id_entidad_fkey') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_id_entidad_fkey FOREIGN KEY (id_entidad) REFERENCES core.entidades(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_id_paciente_fkey') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_id_paciente_fkey FOREIGN KEY (id_paciente) REFERENCES core.pacientes(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_id_recaudo_fkey') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_id_recaudo_fkey FOREIGN KEY (id_recaudo) REFERENCES core.conceptos_recaudo(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'cuentas_medicas_id_sede_fkey') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT cuentas_medicas_id_sede_fkey FOREIGN KEY (id_sede) REFERENCES core.sede(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'direccion_cod_zona_territorial_fkey') THEN
        ALTER TABLE ONLY core.direccion
    ADD CONSTRAINT direccion_cod_zona_territorial_fkey FOREIGN KEY (cod_zona_territorial) REFERENCES core.zonas_territoriales(cod);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'direccion_codigo_municipio_fkey') THEN
        ALTER TABLE ONLY core.direccion
    ADD CONSTRAINT direccion_codigo_municipio_fkey FOREIGN KEY (cod_municipio) REFERENCES core.municipio(cod);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_cod_cobertura_salud_fkey') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_cod_cobertura_salud_fkey FOREIGN KEY (cod_cobertura_salud) REFERENCES core.coberturas_salud(cod);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_cod_responsabilidad_tributaria_fkey') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_cod_responsabilidad_tributaria_fkey FOREIGN KEY (cod_responsabilidad_tributaria) REFERENCES facturacion.tipo_responsabilidad_tributaria(codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_cod_tipo_identificacion_fkey') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_cod_tipo_identificacion_fkey FOREIGN KEY (cod_tipo_identificacion) REFERENCES core.tipo_identificacion(codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_id_contacto_fkey') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_id_contacto_fkey FOREIGN KEY (id_contacto) REFERENCES core.contacto(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_id_direccion_fkey') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_id_direccion_fkey FOREIGN KEY (id_direccion) REFERENCES core.direccion(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'entidades_id_tipo_usuario_fkey') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT entidades_id_tipo_usuario_fkey FOREIGN KEY (id_tipo_usuario) REFERENCES core.tipo_usuario(cod);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_autorizacion_archivo') THEN
        ALTER TABLE ONLY core.autorizaciones
    ADD CONSTRAINT fk_autorizacion_archivo FOREIGN KEY (autorizacion_archivo_id) REFERENCES core.archivos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_concepto_recaudo_comprobante') THEN
        ALTER TABLE ONLY core.conceptos_recaudo
    ADD CONSTRAINT fk_concepto_recaudo_comprobante FOREIGN KEY (comprobante_archivo_id) REFERENCES core.archivos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_cuenta_medica_consentimiento') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT fk_cuenta_medica_consentimiento FOREIGN KEY (consentimiento_archivo_id) REFERENCES core.archivos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_cuentas_medicas_estado') THEN
        ALTER TABLE ONLY core.cuentas_medicas
    ADD CONSTRAINT fk_cuentas_medicas_estado FOREIGN KEY (estado) REFERENCES core.estados_cuenta_medica(codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_entidades_tipo_facturacion') THEN
        ALTER TABLE ONLY core.entidades
    ADD CONSTRAINT fk_entidades_tipo_facturacion FOREIGN KEY (tipo_facturacion) REFERENCES core.tipos_facturacion(codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_profiles_medico') THEN
        ALTER TABLE ONLY core.profiles
    ADD CONSTRAINT fk_profiles_medico FOREIGN KEY (medico_id) REFERENCES hc.medicos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'item_cuenta_medica_id_admision_fkey') THEN
        ALTER TABLE ONLY core.item_cuenta_medica
    ADD CONSTRAINT item_cuenta_medica_id_admision_fkey FOREIGN KEY (id_admision) REFERENCES core.admisiones(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'item_cuenta_medica_id_cuenta_medica_fkey') THEN
        ALTER TABLE ONLY core.item_cuenta_medica
    ADD CONSTRAINT item_cuenta_medica_id_cuenta_medica_fkey FOREIGN KEY (id_cuenta_medica) REFERENCES core.cuentas_medicas(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'item_cuenta_medica_id_procedimiento_fkey') THEN
        ALTER TABLE ONLY core.item_cuenta_medica
    ADD CONSTRAINT item_cuenta_medica_id_procedimiento_fkey FOREIGN KEY (id_procedimiento) REFERENCES servicios_salud.procedimientos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'municipio_cod_departamento_fkey') THEN
        ALTER TABLE ONLY core.municipio
    ADD CONSTRAINT municipio_cod_departamento_fkey FOREIGN KEY (cod_departamento) REFERENCES core.departamento(codigo) ON UPDATE CASCADE ON DELETE RESTRICT;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_audit_log_paciente_id_fkey') THEN
        ALTER TABLE ONLY core.pacientes_audit_log
    ADD CONSTRAINT pacientes_audit_log_paciente_id_fkey FOREIGN KEY (paciente_id) REFERENCES core.pacientes(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_audit_log_performed_by_fkey') THEN
        ALTER TABLE ONLY core.pacientes_audit_log
    ADD CONSTRAINT pacientes_audit_log_performed_by_fkey FOREIGN KEY (performed_by) REFERENCES core.profiles(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_cod_tipo_identificacion_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_cod_tipo_identificacion_fkey FOREIGN KEY (cod_tipo_identificacion) REFERENCES core.tipo_identificacion(codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_created_by_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_created_by_fkey FOREIGN KEY (created_by) REFERENCES core.profiles(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_contacto_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_contacto_fkey FOREIGN KEY (id_contacto) REFERENCES core.contacto(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_direccion_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_direccion_fkey FOREIGN KEY (id_direccion) REFERENCES core.direccion(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_entidad_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_entidad_fkey FOREIGN KEY (id_entidad) REFERENCES core.entidades(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_escolaridad_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_escolaridad_fkey FOREIGN KEY (id_escolaridad) REFERENCES core.escolaridades(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_estado_civil_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_estado_civil_fkey FOREIGN KEY (id_estado_civil) REFERENCES core.estados_civil(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_genero_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_genero_fkey FOREIGN KEY (id_genero) REFERENCES core.generos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_grupo_sanguineo_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_grupo_sanguineo_fkey FOREIGN KEY (id_grupo_sanguineo) REFERENCES core.grupos_sanguineos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_ocupacion_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_ocupacion_fkey FOREIGN KEY (id_ocupacion) REFERENCES core.ocupaciones(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_id_pais_origen_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_id_pais_origen_fkey FOREIGN KEY (id_pais_origen) REFERENCES core.paises(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'pacientes_updated_by_fkey') THEN
        ALTER TABLE ONLY core.pacientes
    ADD CONSTRAINT pacientes_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES core.profiles(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'profiles_role_id_fkey') THEN
        ALTER TABLE ONLY core.profiles
    ADD CONSTRAINT profiles_role_id_fkey FOREIGN KEY (role_id) REFERENCES core.roles(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'refresh_tokens_profile_id_fkey') THEN
        ALTER TABLE ONLY core.refresh_tokens
    ADD CONSTRAINT refresh_tokens_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES core.profiles(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'sede_direccion_fkey') THEN
        ALTER TABLE ONLY core.sede
    ADD CONSTRAINT sede_direccion_fkey FOREIGN KEY (direccion) REFERENCES core.direccion(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'antecedentes_historia_id_fkey') THEN
        ALTER TABLE ONLY hc.antecedentes
    ADD CONSTRAINT antecedentes_historia_id_fkey FOREIGN KEY (historia_id) REFERENCES hc.historias_clinicas(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'controles_consulta_historia_id_fkey') THEN
        ALTER TABLE ONLY hc.controles_consulta
    ADD CONSTRAINT controles_consulta_historia_id_fkey FOREIGN KEY (historia_id) REFERENCES hc.historias_clinicas(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'examen_fisico_control_id_fkey') THEN
        ALTER TABLE ONLY hc.examen_fisico
    ADD CONSTRAINT examen_fisico_control_id_fkey FOREIGN KEY (control_id) REFERENCES hc.controles_consulta(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_hc_historia_medico') THEN
        ALTER TABLE ONLY hc.historias_clinicas
    ADD CONSTRAINT fk_hc_historia_medico FOREIGN KEY (medico_id) REFERENCES hc.medicos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_hc_historia_paciente') THEN
        ALTER TABLE ONLY hc.historias_clinicas
    ADD CONSTRAINT fk_hc_historia_paciente FOREIGN KEY (paciente_id) REFERENCES core.pacientes(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'fk_medicos_tipo_doc') THEN
        ALTER TABLE ONLY hc.medicos
    ADD CONSTRAINT fk_medicos_tipo_doc FOREIGN KEY (tipo_doc) REFERENCES core.tipo_identificacion(codigo);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'formulas_medicas_control_id_fkey') THEN
        ALTER TABLE ONLY hc.formulas_medicas
    ADD CONSTRAINT formulas_medicas_control_id_fkey FOREIGN KEY (control_id) REFERENCES hc.controles_consulta(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'indicaciones_control_id_fkey') THEN
        ALTER TABLE ONLY hc.indicaciones
    ADD CONSTRAINT indicaciones_control_id_fkey FOREIGN KEY (control_id) REFERENCES hc.controles_consulta(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'juntas_medicas_control_id_fkey') THEN
        ALTER TABLE ONLY hc.juntas_medicas
    ADD CONSTRAINT juntas_medicas_control_id_fkey FOREIGN KEY (control_id) REFERENCES hc.controles_consulta(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'juntas_medicas_especialidad_id_fkey') THEN
        ALTER TABLE ONLY hc.juntas_medicas
    ADD CONSTRAINT juntas_medicas_especialidad_id_fkey FOREIGN KEY (especialidad_id) REFERENCES hc.medico_especialidades(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'juntas_medicas_medico_id_fkey') THEN
        ALTER TABLE ONLY hc.juntas_medicas
    ADD CONSTRAINT juntas_medicas_medico_id_fkey FOREIGN KEY (medico_id) REFERENCES hc.medicos(id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'medico_especialidades_medico_id_fkey') THEN
        ALTER TABLE ONLY hc.medico_especialidades
    ADD CONSTRAINT medico_especialidades_medico_id_fkey FOREIGN KEY (medico_id) REFERENCES hc.medicos(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'revision_sistemas_control_id_fkey') THEN
        ALTER TABLE ONLY hc.revision_sistemas
    ADD CONSTRAINT revision_sistemas_control_id_fkey FOREIGN KEY (control_id) REFERENCES hc.controles_consulta(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'signos_vitales_control_id_fkey') THEN
        ALTER TABLE ONLY hc.signos_vitales
    ADD CONSTRAINT signos_vitales_control_id_fkey FOREIGN KEY (control_id) REFERENCES hc.controles_consulta(id) ON DELETE CASCADE;
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conname = 'solicitudes_control_id_fkey') THEN
        ALTER TABLE ONLY hc.solicitudes
    ADD CONSTRAINT solicitudes_control_id_fkey FOREIGN KEY (control_id) REFERENCES hc.controles_consulta(id) ON DELETE CASCADE;
    END IF;
END;
$$;

ALTER TABLE appointments.appointment_audit_log ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.appointment_participants ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.appointment_resource_allocations ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.appointment_state_history ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.appointments ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.domain_events ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.facility_operating_hours ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.facility_resources ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.n8n_idempotency_keys ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.notifications ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.pqrs ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.schedule_blocks ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.schedule_plan_blocks ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.schedule_plan_slots ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.schedule_plans ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.schedules ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.sede_code_aliases ENABLE ROW LEVEL SECURITY;

ALTER TABLE appointments.specialist_metadata ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.admisiones ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.appointment_admissions ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.autorizaciones ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.coberturas_salud ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.conceptos_recaudo ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.contacto ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.contactos_paciente ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.cuentas_medicas ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.departamento ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.direccion ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.entidades ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.escolaridades ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.estados_civil ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.generos ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.grupos_sanguineos ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.item_cuenta_medica ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.municipio ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.ocupaciones ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.pacientes ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.paises ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.parentescos ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.profiles ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.roles ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.sede ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.tipo_identificacion ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.tipo_recaudo ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.tipo_usuario ENABLE ROW LEVEL SECURITY;

ALTER TABLE core.zonas_territoriales ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.antecedentes ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.catalogo_cie10 ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.catalogo_medicamentos ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.controles_consulta ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.examen_fisico ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.formulas_medicas ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.historias_clinicas ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.indicaciones ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.juntas_medicas ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.medico_especialidades ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.medicos ENABLE ROW LEVEL SECURITY;

CREATE POLICY read_medicos ON hc.medicos FOR SELECT USING (true);

ALTER TABLE hc.revision_sistemas ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.signos_vitales ENABLE ROW LEVEL SECURITY;

ALTER TABLE hc.solicitudes ENABLE ROW LEVEL SECURITY;

-- PostgreSQL database dump complete

\unrestrict 1IvfKe5klUSclMMQqUa9GqFactf7dc6b8mjUKqRGy2kZUCd86mrWvDPKRF5x50J

