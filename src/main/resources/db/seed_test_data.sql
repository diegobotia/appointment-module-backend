-- =====================================================================
-- SQL SEED SCRIPT FOR IPS CENTIR APPOINTMENTS MODULE
-- Descripcion: Inserta datos de prueba realistas para local y Supabase.
--              Este script es idempotente y seguro de ejecutar repetidamente.
-- =====================================================================

-- 1. LIMPIEZA DE DATOS EXISTENTES DE PRUEBA (Para evitar duplicados)
-- Borrar dependencias operacionales de citas de prueba primero
DELETE FROM appointments.appointment_state_history 
WHERE appointment_id IN (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03'
);
DELETE FROM appointments.appointments 
WHERE id IN (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03'
);

-- Borrar bloqueos y agendas de prueba
DELETE FROM appointments.schedule_blocks 
WHERE schedule_id IN (
    'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
    'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02'
);
DELETE FROM appointments.schedules 
WHERE id IN (
    'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
    'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02'
);

-- Borrar metadata de especialistas
DELETE FROM appointments.specialist_metadata 
WHERE profile_id IN (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05'
);

-- Borrar medicos de prueba de hc.medicos (tabla real en Supabase)
DELETE FROM hc.medicos WHERE id IN (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05'
) OR num_doc IN ('10001', '10002', '10003', '10004', '10005');

-- Borrar perfiles de prueba
DELETE FROM core.profiles WHERE id IN (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05'
) OR email IN (
    'admin@ipscentir.com', 
    'admisiones@ipscentir.com', 
    'facturacion@ipscentir.com', 
    'carlos.rodriguez@ips.test', 
    'ana.martinez@ips.test', 
    'roberto.lopez@ips.test', 
    'maria.garcia@ips.test', 
    'juan.hernandez@ips.test'
);

-- Borrar pacientes de prueba
DELETE FROM core.pacientes WHERE num_identificacion IN ('12345678', '87654321');
DELETE FROM core.contacto WHERE email IN ('juan.perez@example.com', 'maria.rod@example.com');
DELETE FROM core.direccion WHERE detalle IN ('Calle 10 # 5-20', 'Avenida Norte # 45-12');

-- Borrar PQRS de prueba
DELETE FROM appointments.pqrs WHERE radicado IN ('PQRS-202605-001', 'PQRS-202605-002');


-- 2. INSERTAR ROLES (Garantizar roles base en core)
INSERT INTO core.roles (id, nombre, descripcion)
VALUES 
    ('70eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Administracion', 'Acceso total al panel administrativo'),
    ('70eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'Admisiones', 'Gestión diaria de admisiones y citas'),
    ('70eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'Asesor', 'Call center: gestión del ciclo de vida de citas'),
    ('70eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'Medico', 'Acceso a agendas y consultas médicas'),
    ('70eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'Facturacion', 'Lectura de citas para cuentas médicas')
ON CONFLICT (nombre) DO UPDATE SET descripcion = EXCLUDED.descripcion;


-- 3. INSERTAR PERFILES DE STAFF (Administración, Admisiones, Facturación)
INSERT INTO core.profiles (id, role_id, name, email, password_change_required, esta_activo)
VALUES 
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 
        (SELECT id FROM core.roles WHERE nombre = 'Administracion' LIMIT 1), 
        'Admin IPS Centir', 
        'admin@ipscentir.com', 
        false, 
        true
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 
        (SELECT id FROM core.roles WHERE nombre = 'Admisiones' LIMIT 1), 
        'Admisiones IPS Centir', 
        'admisiones@ipscentir.com', 
        false, 
        true
    ),
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 
        (SELECT id FROM core.roles WHERE nombre = 'Facturacion' LIMIT 1), 
        'Facturacion IPS Centir', 
        'facturacion@ipscentir.com', 
        false, 
        true
    )
ON CONFLICT (id) DO NOTHING;


-- 4. INSERTAR MÉDICOS EN HC.MEDICOS
INSERT INTO hc.medicos (
    id, 
    nombre, 
    apellido, 
    tipo_doc, 
    num_doc, 
    registro, 
    especialidad, 
    activo,
    created_at,
    updated_at
)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Carlos', 'Rodriguez', 'CC', '10001', 'MED001', 'Medico Fisiatra', true, NOW(), NOW()),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'Ana', 'Martinez', 'CC', '10002', 'MED002', 'Dolor', true, NOW(), NOW()),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'Roberto', 'Lopez', 'CC', '10003', 'Medicina Laboral', true, NOW(), NOW()),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'Maria', 'Garcia', 'CC', '10004', 'Psicologo', true, NOW(), NOW()),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'Juan', 'Hernandez', 'CC', '10005', 'Psiquiatra', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;


-- 5. INSERTAR PERFILES DE MÉDICOS (Para autenticación y roles)
INSERT INTO core.profiles (id, role_id, name, email, password_change_required, esta_activo)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', (SELECT id FROM core.roles WHERE nombre = 'Medico' LIMIT 1), 'Dr. Carlos Rodriguez', 'carlos.rodriguez@ips.test', false, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', (SELECT id FROM core.roles WHERE nombre = 'Medico' LIMIT 1), 'Dra. Ana Martinez', 'ana.martinez@ips.test', false, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', (SELECT id FROM core.roles WHERE nombre = 'Medico' LIMIT 1), 'Dr. Roberto Lopez', 'roberto.lopez@ips.test', false, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', (SELECT id FROM core.roles WHERE nombre = 'Medico' LIMIT 1), 'Dra. Maria Garcia', 'maria.garcia@ips.test', false, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', (SELECT id FROM core.roles WHERE nombre = 'Medico' LIMIT 1), 'Dr. Juan Hernandez', 'juan.hernandez@ips.test', false, true)
ON CONFLICT (id) DO NOTHING;


-- 6. INSERTAR ESPECIALIDADES METADATA EN APPOINTMENTS
INSERT INTO appointments.specialist_metadata (profile_id, specialties_json, max_patients_per_slot, is_active, synced_from_hc)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '{"primary": "FISIOTERAPIA", "secondary": ["REHABILITACION"], "available": ["FISIOTERAPIA", "REHABILITACION"]}'::jsonb, 4, true, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '{"primary": "DOLOR", "secondary": ["MEDICINA_GENERAL"], "available": ["DOLOR"]}'::jsonb, 1, true, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '{"primary": "MEDICINA_LABORAL", "secondary": [], "available": ["MEDICINA_LABORAL"]}'::jsonb, 1, true, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '{"primary": "PSICOLOGIA", "secondary": ["TERAPIA_GRUPO"], "available": ["PSICOLOGIA", "TERAPIA_GRUPO"]}'::jsonb, 6, true, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', '{"primary": "PSIQUIATRIA", "secondary": [], "available": ["PSIQUIATRIA"]}'::jsonb, 1, true, true)
ON CONFLICT (profile_id) DO NOTHING;


-- 7. INSERTAR SEDES (FACILITIES)
INSERT INTO appointments.facilities (id, code, name, address, is_active)
VALUES
    ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380f01', 'SEDE_CONQUISTADORES', 'Sede Conquistadores', 'Calle 34 # 63-56, barrio Conquistadores', true),
    ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380f02', 'SEDE_BELEN', 'Sede Belén', 'Cra 82A # 28-39, Belén los Alpes', true)
ON CONFLICT (code) DO UPDATE SET address = EXCLUDED.address, name = EXCLUDED.name;

INSERT INTO appointments.facility_code_aliases (facility_id, alias_code)
SELECT f.id, 'SEDE_PRINCIPAL'
FROM appointments.facilities f WHERE f.code = 'SEDE_CONQUISTADORES'
ON CONFLICT (alias_code) DO NOTHING;

INSERT INTO appointments.facility_code_aliases (facility_id, alias_code)
SELECT f.id, 'SEDE_NORTE'
FROM appointments.facilities f WHERE f.code = 'SEDE_BELEN'
ON CONFLICT (alias_code) DO NOTHING;

-- 7b. Horario institucional e inventario físico (idempotente tras V24/V25)
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


-- 8. PACIENTES: DIRECCIONES Y CONTACTOS
INSERT INTO core.contacto (id, email, telefono)
VALUES 
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'juan.perez@example.com', '3151234567'),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 'maria.rod@example.com', '3187654321')
ON CONFLICT (email) DO NOTHING;

INSERT INTO core.direccion (id, cod_municipio, cod_zona_territorial, detalle, barrio)
VALUES 
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380d01', 'MEDELLIN', 'URBANA', 'Calle 10 # 5-20', 'El Poblado'),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380d02', 'MEDELLIN', 'URBANA', 'Avenida Norte # 45-12', 'Robledo')
ON CONFLICT DO NOTHING;


-- 9. PACIENTES MAESTROS
INSERT INTO core.pacientes (
    id, 
    nombres, 
    apellidos, 
    num_identificacion, 
    cod_tipo_identificacion, 
    fecha_nacimiento, 
    id_genero, 
    id_estado_civil, 
    id_ocupacion, 
    id_direccion, 
    id_contacto, 
    id_grupo_sanguineo, 
    id_escolaridad, 
    estrato, 
    id_pais_origen
)
VALUES 
    (
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 
        'Juan', 
        'Perez', 
        '12345678', 
        'CC', 
        '1990-05-15', 
        '82c11edc-6662-4098-95f8-6f21f9f07263', -- Genero
        '60b62af5-6f3a-463f-bedc-13b299c09092', -- Estado Civil
        '783b485b-95b4-45bb-aa6b-e981893cb002', -- Ocupacion
        'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380d01', 
        'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 
        'e1788483-80ef-4faa-8cd7-b75edfeeb8b6', -- Grupo Sanguineo
        '5c32fcf3-e37c-4e18-9ae8-ad52e2bb7cce', -- Escolaridad
        3, 
        170 -- Colombia
    ),
    (
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b02', 
        'Maria', 
        'Rodriguez', 
        '87654321', 
        'CC', 
        '1995-10-22', 
        '82c11edc-6662-4098-95f8-6f21f9f07263', 
        '60b62af5-6f3a-463f-bedc-13b299c09092', 
        '783b485b-95b4-45bb-aa6b-e981893cb002', 
        'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380d02', 
        'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 
        'e1788483-80ef-4faa-8cd7-b75edfeeb8b6', 
        '5c32fcf3-e37c-4e18-9ae8-ad52e2bb7cce', 
        2, 
        170
    )
ON CONFLICT (num_identificacion) DO NOTHING;


-- 10. HORARIOS (SCHEDULES) PARA AGENDAMIENTO
INSERT INTO appointments.schedules (
    id, 
    doctor_id, 
    facility_id, 
    specialty, 
    day_of_week, 
    start_time, 
    end_time, 
    slot_duration_minutes, 
    max_patients_per_slot, 
    is_active
)
VALUES 
    (
        'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', -- Dr. Carlos
        'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380f01', -- Sede Principal
        'Fisiatria', 
        1, -- Lunes
        '08:00:00', 
        '12:00:00', 
        30, 
        4, -- Terapia grupal / multisector
        true
    ),
    (
        'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', -- Dra. Ana
        'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380f02', -- Sede Norte
        'Dolor', 
        2, -- Martes
        '14:00:00', 
        '18:00:00', 
        30, 
        1, 
        true
    )
ON CONFLICT (id) DO NOTHING;


-- 11. CITAS DE PRUEBA (PAST, TODAY, FUTURE)
-- Cita en el Pasado (Completada)
INSERT INTO appointments.appointments (
    id, 
    patient_id, 
    doctor_id, 
    schedule_id, 
    facility_id, 
    appointment_date, 
    appointment_time, 
    duration_minutes, 
    appointment_type, 
    status, 
    specialty, 
    reason, 
    notes
)
VALUES 
    (
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', -- Juan Perez
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', -- Dr. Carlos
        'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
        'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380f01', 
        '2026-05-18', -- Pasado
        '09:00:00', 
        30, 
        'PRESENCIAL', 
        'COMPLETED', 
        'Fisiatria', 
        'Dolor lumbar cronico', 
        'Paciente muestra mejoria'
    )
ON CONFLICT (id) DO NOTHING;

-- Cita para Hoy (Programada)
INSERT INTO appointments.appointments (
    id, 
    patient_id, 
    doctor_id, 
    schedule_id, 
    facility_id, 
    appointment_date, 
    appointment_time, 
    duration_minutes, 
    appointment_type, 
    status, 
    specialty, 
    reason, 
    notes
)
VALUES 
    (
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b02', -- Maria Rodriguez
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', -- Dr. Carlos
        'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 
        'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380f01', 
        CURRENT_DATE, -- Hoy
        '11:00:00', 
        30, 
        'PRESENCIAL', 
        'SCHEDULED', 
        'Fisiatria', 
        'Terapia de hombro', 
        'Cita de control rutinaria'
    )
ON CONFLICT (id) DO NOTHING;

-- Cita para el Futuro (Confirmada)
INSERT INTO appointments.appointments (
    id, 
    patient_id, 
    doctor_id, 
    schedule_id, 
    facility_id, 
    appointment_date, 
    appointment_time, 
    duration_minutes, 
    appointment_type, 
    status, 
    specialty, 
    reason, 
    notes
)
VALUES 
    (
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', -- Juan Perez
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', -- Dra. Ana
        'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 
        'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380f02', 
        CURRENT_DATE + 3, -- En 3 dias
        '15:00:00', 
        30, 
        'PRESENCIAL', 
        'CONFIRMED', 
        'Dolor', 
        'Migrañas recurrentes', 
        'Revisión de dosis terapeutica'
    )
ON CONFLICT (id) DO NOTHING;


-- 12. PQRS DE PRUEBA
INSERT INTO appointments.pqrs (
    id, 
    cedula, 
    tipo, 
    descripcion, 
    correo, 
    nombres, 
    telefono, 
    radicado, 
    status, 
    metadata, 
    created_at, 
    updated_at
)
VALUES 
    (
        '90eebc99-9c0b-4ef8-bb6d-6bb9bd380f01', 
        '12345678', 
        'RECLAMO', 
        'El paciente indica que espero más de 45 minutos para ser atendido por el especialista.', 
        'juan.perez@example.com', 
        'Juan Perez', 
        '3151234567', 
        'PQRS-202605-001', 
        'CREADO', 
        '{"origen": "WEB_FORM"}', 
        CURRENT_TIMESTAMP - INTERVAL '2 days', 
        CURRENT_TIMESTAMP - INTERVAL '2 days'
    ),
    (
        '90eebc99-9c0b-4ef8-bb6d-6bb9bd380f02', 
        '87654321', 
        'SUGERENCIA', 
        'Habilitar más canales de atencion e integrar recordatorios automatizados por WhatsApp.', 
        'maria.rod@example.com', 
        'Maria Rodriguez', 
        '3187654321', 
        'PQRS-202605-002', 
        'EN_PROCESO', 
        '{"origen": "CHAT"}', 
        CURRENT_TIMESTAMP - INTERVAL '1 day', 
        CURRENT_TIMESTAMP
    )
ON CONFLICT (radicado) DO NOTHING;
