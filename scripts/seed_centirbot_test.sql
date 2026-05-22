CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_role_medico UUID;
    v_template_patient core.pacientes%ROWTYPE;
    v_template_address core.direccion%ROWTYPE;

    v_facility_conquistadores_id UUID;
    v_facility_belen_id UUID;

    v_patient_ana_id UUID := '33333333-3333-3333-3333-333333333333';
    v_patient_carlos_id UUID := '33333333-3333-3333-3333-333333333334';
    v_patient_pedro_id UUID := '33333333-3333-3333-3333-333333333335';
    v_patient_marta_id UUID := '33333333-3333-3333-3333-333333333336';
    v_patient_jose_id UUID := '33333333-3333-3333-3333-333333333337';
    v_patient_luisa_id UUID := '33333333-3333-3333-3333-333333333338';

    v_doc_ana TEXT := '9900001001';
    v_doc_carlos TEXT := '9900001002';
    v_doc_pedro TEXT := '9900001003';
    v_doc_marta TEXT := '9900001004';
    v_doc_jose TEXT := '9900001005';
    v_doc_luisa TEXT := '9900001006';

    v_doctor_laura_id TEXT := 'SEED-HCMED-001';
    v_doctor_mateo_id TEXT := 'SEED-HCMED-002';
    v_doctor_sara_id TEXT := 'SEED-HCMED-003';

    v_schedule_therapy_belen_id UUID := '44444444-4444-4444-4444-444444444444';
    v_schedule_psych_conq_id UUID := '44444444-4444-4444-4444-444444444445';
    v_schedule_occ_belen_id UUID := '44444444-4444-4444-4444-444444444446';

    v_target_date_therapy DATE := CURRENT_DATE + 3;
    v_target_date_psych DATE := CURRENT_DATE + 5;
    v_target_date_occ DATE := CURRENT_DATE + 6;

    v_day_of_week_therapy INTEGER := EXTRACT(ISODOW FROM CURRENT_DATE + 3)::INT;
    v_day_of_week_psych INTEGER := EXTRACT(ISODOW FROM CURRENT_DATE + 5)::INT;
    v_day_of_week_occ INTEGER := EXTRACT(ISODOW FROM CURRENT_DATE + 6)::INT;
BEGIN
    SELECT id
    INTO v_role_medico
    FROM core.roles
    WHERE nombre = 'Medico'
    LIMIT 1;

    IF v_role_medico IS NULL THEN
        RAISE EXCEPTION 'Seed seguro abortado: no existe el rol Medico en core.roles';
    END IF;

    SELECT *
    INTO v_template_patient
    FROM core.pacientes
    ORDER BY id
    LIMIT 1;

    IF v_template_patient.id IS NULL THEN
        RAISE EXCEPTION 'Seed seguro abortado: se requiere al menos 1 paciente real o de base para reutilizar catalogos obligatorios de core.pacientes';
    END IF;

    SELECT d.*
    INTO v_template_address
    FROM core.direccion d
    JOIN core.pacientes p ON p.id_direccion = d.id
    WHERE p.id = v_template_patient.id
    LIMIT 1;

    IF v_template_address.id IS NULL THEN
        RAISE EXCEPTION 'Seed seguro abortado: no fue posible resolver una direccion plantilla valida desde el paciente base';
    END IF;

    INSERT INTO hc.medicos (id, nombre, apellido, tipo_doc, num_doc, registro, especialidad, activo)
    SELECT v_doctor_laura_id, 'Laura', 'Mesa', 'CC', '8800001001', 'SEED-MED-001', 'Terapia fisica', true
    WHERE NOT EXISTS (
        SELECT 1 FROM hc.medicos
        WHERE id = v_doctor_laura_id OR registro = 'SEED-MED-001' OR num_doc = '8800001001'
    );

    INSERT INTO hc.medicos (id, nombre, apellido, tipo_doc, num_doc, registro, especialidad, activo)
    SELECT v_doctor_mateo_id, 'Mateo', 'Quintero', 'CC', '8800001002', 'SEED-MED-002', 'Psicologia', true
    WHERE NOT EXISTS (
        SELECT 1 FROM hc.medicos
        WHERE id = v_doctor_mateo_id OR registro = 'SEED-MED-002' OR num_doc = '8800001002'
    );

    INSERT INTO hc.medicos (id, nombre, apellido, tipo_doc, num_doc, registro, especialidad, activo)
    SELECT v_doctor_sara_id, 'Sara', 'Velez', 'CC', '8800001003', 'SEED-MED-003', 'Terapia ocupacional', true
    WHERE NOT EXISTS (
        SELECT 1 FROM hc.medicos
        WHERE id = v_doctor_sara_id OR registro = 'SEED-MED-003' OR num_doc = '8800001003'
    );

    IF NOT EXISTS (SELECT 1 FROM hc.medicos WHERE id = v_doctor_laura_id AND activo = true) THEN
        RAISE EXCEPTION 'Seed seguro abortado: no fue posible asegurar el medico de prueba Laura Mesa en hc.medicos';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM hc.medicos WHERE id = v_doctor_mateo_id AND activo = true) THEN
        RAISE EXCEPTION 'Seed seguro abortado: no fue posible asegurar el medico de prueba Mateo Quintero en hc.medicos';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM hc.medicos WHERE id = v_doctor_sara_id AND activo = true) THEN
        RAISE EXCEPTION 'Seed seguro abortado: no fue posible asegurar el medico de prueba Sara Velez en hc.medicos';
    END IF;

    INSERT INTO appointments.facilities (id, code, name, address, is_active)
    SELECT 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'SEDE_PRINCIPAL', 'Sede Conquistadores', 'Calle 34 N 63-56', true
    WHERE NOT EXISTS (
        SELECT 1
        FROM appointments.facilities
        WHERE code = 'SEDE_PRINCIPAL'
    );

    INSERT INTO appointments.facilities (id, code, name, address, is_active)
    SELECT 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'SEDE_NORTE', 'Sede Belen', 'Cra 82A 28-39', true
    WHERE NOT EXISTS (
        SELECT 1
        FROM appointments.facilities
        WHERE code = 'SEDE_NORTE'
    );

    SELECT id INTO v_facility_conquistadores_id
    FROM appointments.facilities
    WHERE code = 'SEDE_PRINCIPAL'
    LIMIT 1;

    SELECT id INTO v_facility_belen_id
    FROM appointments.facilities
    WHERE code = 'SEDE_NORTE'
    LIMIT 1;

    IF v_facility_conquistadores_id IS NULL OR v_facility_belen_id IS NULL THEN
        RAISE EXCEPTION 'Seed seguro abortado: no fue posible resolver las sedes SEDE_PRINCIPAL y SEDE_NORTE';
    END IF;

    INSERT INTO core.contacto (id, email, telefono)
    SELECT '11111111-1111-1111-1111-111111111111', 'centirbot.test.ana+seed@ipscentir.local', '3200001001'
    WHERE NOT EXISTS (SELECT 1 FROM core.contacto WHERE id = '11111111-1111-1111-1111-111111111111');
    INSERT INTO core.contacto (id, email, telefono)
    SELECT '11111111-1111-1111-1111-111111111112', 'centirbot.test.carlos+seed@ipscentir.local', '3200001002'
    WHERE NOT EXISTS (SELECT 1 FROM core.contacto WHERE id = '11111111-1111-1111-1111-111111111112');
    INSERT INTO core.contacto (id, email, telefono)
    SELECT '11111111-1111-1111-1111-111111111113', 'centirbot.test.pedro+seed@ipscentir.local', '3200001003'
    WHERE NOT EXISTS (SELECT 1 FROM core.contacto WHERE id = '11111111-1111-1111-1111-111111111113');
    INSERT INTO core.contacto (id, email, telefono)
    SELECT '11111111-1111-1111-1111-111111111114', 'centirbot.test.marta+seed@ipscentir.local', '3200001004'
    WHERE NOT EXISTS (SELECT 1 FROM core.contacto WHERE id = '11111111-1111-1111-1111-111111111114');
    INSERT INTO core.contacto (id, email, telefono)
    SELECT '11111111-1111-1111-1111-111111111115', 'centirbot.test.jose+seed@ipscentir.local', '3200001005'
    WHERE NOT EXISTS (SELECT 1 FROM core.contacto WHERE id = '11111111-1111-1111-1111-111111111115');
    INSERT INTO core.contacto (id, email, telefono)
    SELECT '11111111-1111-1111-1111-111111111116', 'centirbot.test.luisa+seed@ipscentir.local', '3200001006'
    WHERE NOT EXISTS (SELECT 1 FROM core.contacto WHERE id = '11111111-1111-1111-1111-111111111116');

    INSERT INTO core.direccion (id, cod_municipio, cod_zona_territorial, detalle, barrio)
    SELECT '22222222-2222-2222-2222-222222222221', v_template_address.cod_municipio, v_template_address.cod_zona_territorial, 'Calle 10 #20-30', 'Laureles'
    WHERE NOT EXISTS (SELECT 1 FROM core.direccion WHERE id = '22222222-2222-2222-2222-222222222221');
    INSERT INTO core.direccion (id, cod_municipio, cod_zona_territorial, detalle, barrio)
    SELECT '22222222-2222-2222-2222-222222222222', v_template_address.cod_municipio, v_template_address.cod_zona_territorial, 'Carrera 70 #45-11', 'Estadio'
    WHERE NOT EXISTS (SELECT 1 FROM core.direccion WHERE id = '22222222-2222-2222-2222-222222222222');
    INSERT INTO core.direccion (id, cod_municipio, cod_zona_territorial, detalle, barrio)
    SELECT '22222222-2222-2222-2222-222222222223', v_template_address.cod_municipio, v_template_address.cod_zona_territorial, 'Calle 44 #76-90', 'Belen'
    WHERE NOT EXISTS (SELECT 1 FROM core.direccion WHERE id = '22222222-2222-2222-2222-222222222223');
    INSERT INTO core.direccion (id, cod_municipio, cod_zona_territorial, detalle, barrio)
    SELECT '22222222-2222-2222-2222-222222222224', v_template_address.cod_municipio, v_template_address.cod_zona_territorial, 'Carrera 65 #48-25', 'Floresta'
    WHERE NOT EXISTS (SELECT 1 FROM core.direccion WHERE id = '22222222-2222-2222-2222-222222222224');
    INSERT INTO core.direccion (id, cod_municipio, cod_zona_territorial, detalle, barrio)
    SELECT '22222222-2222-2222-2222-222222222225', v_template_address.cod_municipio, v_template_address.cod_zona_territorial, 'Calle 50 #74-18', 'Suramericana'
    WHERE NOT EXISTS (SELECT 1 FROM core.direccion WHERE id = '22222222-2222-2222-2222-222222222225');
    INSERT INTO core.direccion (id, cod_municipio, cod_zona_territorial, detalle, barrio)
    SELECT '22222222-2222-2222-2222-222222222226', v_template_address.cod_municipio, v_template_address.cod_zona_territorial, 'Carrera 78 #33-12', 'La Castellana'
    WHERE NOT EXISTS (SELECT 1 FROM core.direccion WHERE id = '22222222-2222-2222-2222-222222222226');

    INSERT INTO core.pacientes (
        id, nombres, apellidos, num_identificacion, cod_tipo_identificacion, fecha_nacimiento,
        id_genero, id_estado_civil, id_ocupacion, id_direccion, id_contacto,
        id_grupo_sanguineo, id_escolaridad, estrato, id_pais_origen
    )
    SELECT
        v_patient_ana_id, 'Ana Maria', 'Lopez', v_doc_ana, v_template_patient.cod_tipo_identificacion, DATE '1994-08-15',
        v_template_patient.id_genero, v_template_patient.id_estado_civil, v_template_patient.id_ocupacion,
        '22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111',
        v_template_patient.id_grupo_sanguineo, v_template_patient.id_escolaridad, 3, v_template_patient.id_pais_origen
    WHERE NOT EXISTS (SELECT 1 FROM core.pacientes WHERE id = v_patient_ana_id OR num_identificacion = v_doc_ana);

    INSERT INTO core.pacientes (
        id, nombres, apellidos, num_identificacion, cod_tipo_identificacion, fecha_nacimiento,
        id_genero, id_estado_civil, id_ocupacion, id_direccion, id_contacto,
        id_grupo_sanguineo, id_escolaridad, estrato, id_pais_origen
    )
    SELECT
        v_patient_carlos_id, 'Carlos Andres', 'Ruiz', v_doc_carlos, v_template_patient.cod_tipo_identificacion, DATE '1989-03-20',
        v_template_patient.id_genero, v_template_patient.id_estado_civil, v_template_patient.id_ocupacion,
        '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111112',
        v_template_patient.id_grupo_sanguineo, v_template_patient.id_escolaridad, 2, v_template_patient.id_pais_origen
    WHERE NOT EXISTS (SELECT 1 FROM core.pacientes WHERE id = v_patient_carlos_id OR num_identificacion = v_doc_carlos);

    INSERT INTO core.pacientes (
        id, nombres, apellidos, num_identificacion, cod_tipo_identificacion, fecha_nacimiento,
        id_genero, id_estado_civil, id_ocupacion, id_direccion, id_contacto,
        id_grupo_sanguineo, id_escolaridad, estrato, id_pais_origen
    )
    SELECT
        v_patient_pedro_id, 'Pedro Jose', 'Diaz', v_doc_pedro, v_template_patient.cod_tipo_identificacion, DATE '1991-11-05',
        v_template_patient.id_genero, v_template_patient.id_estado_civil, v_template_patient.id_ocupacion,
        '22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111113',
        v_template_patient.id_grupo_sanguineo, v_template_patient.id_escolaridad, 2, v_template_patient.id_pais_origen
    WHERE NOT EXISTS (SELECT 1 FROM core.pacientes WHERE id = v_patient_pedro_id OR num_identificacion = v_doc_pedro);

    INSERT INTO core.pacientes (
        id, nombres, apellidos, num_identificacion, cod_tipo_identificacion, fecha_nacimiento,
        id_genero, id_estado_civil, id_ocupacion, id_direccion, id_contacto,
        id_grupo_sanguineo, id_escolaridad, estrato, id_pais_origen
    )
    SELECT
        v_patient_marta_id, 'Marta Lucia', 'Rios', v_doc_marta, v_template_patient.cod_tipo_identificacion, DATE '1985-06-17',
        v_template_patient.id_genero, v_template_patient.id_estado_civil, v_template_patient.id_ocupacion,
        '22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111114',
        v_template_patient.id_grupo_sanguineo, v_template_patient.id_escolaridad, 4, v_template_patient.id_pais_origen
    WHERE NOT EXISTS (SELECT 1 FROM core.pacientes WHERE id = v_patient_marta_id OR num_identificacion = v_doc_marta);

    INSERT INTO core.pacientes (
        id, nombres, apellidos, num_identificacion, cod_tipo_identificacion, fecha_nacimiento,
        id_genero, id_estado_civil, id_ocupacion, id_direccion, id_contacto,
        id_grupo_sanguineo, id_escolaridad, estrato, id_pais_origen
    )
    SELECT
        v_patient_jose_id, 'Jose Manuel', 'Paz', v_doc_jose, v_template_patient.cod_tipo_identificacion, DATE '1990-01-12',
        v_template_patient.id_genero, v_template_patient.id_estado_civil, v_template_patient.id_ocupacion,
        '22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111115',
        v_template_patient.id_grupo_sanguineo, v_template_patient.id_escolaridad, 3, v_template_patient.id_pais_origen
    WHERE NOT EXISTS (SELECT 1 FROM core.pacientes WHERE id = v_patient_jose_id OR num_identificacion = v_doc_jose);

    INSERT INTO core.pacientes (
        id, nombres, apellidos, num_identificacion, cod_tipo_identificacion, fecha_nacimiento,
        id_genero, id_estado_civil, id_ocupacion, id_direccion, id_contacto,
        id_grupo_sanguineo, id_escolaridad, estrato, id_pais_origen
    )
    SELECT
        v_patient_luisa_id, 'Luisa Fernanda', 'Gomez', v_doc_luisa, v_template_patient.cod_tipo_identificacion, DATE '1996-12-01',
        v_template_patient.id_genero, v_template_patient.id_estado_civil, v_template_patient.id_ocupacion,
        '22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111116',
        v_template_patient.id_grupo_sanguineo, v_template_patient.id_escolaridad, 3, v_template_patient.id_pais_origen
    WHERE NOT EXISTS (SELECT 1 FROM core.pacientes WHERE id = v_patient_luisa_id OR num_identificacion = v_doc_luisa);

    INSERT INTO appointments.schedules (
        id, doctor_id, facility_id, specialty, day_of_week, start_time, end_time,
        slot_duration_minutes, max_patients_per_slot, is_active
    )
    SELECT v_schedule_therapy_belen_id, v_doctor_laura_id, v_facility_belen_id, 'Terapia fisica', v_day_of_week_therapy, TIME '08:00', TIME '12:00', 30, 3, true
    WHERE NOT EXISTS (SELECT 1 FROM appointments.schedules WHERE id = v_schedule_therapy_belen_id);

    INSERT INTO appointments.schedules (
        id, doctor_id, facility_id, specialty, day_of_week, start_time, end_time,
        slot_duration_minutes, max_patients_per_slot, is_active
    )
    SELECT v_schedule_psych_conq_id, v_doctor_mateo_id, v_facility_conquistadores_id, 'Psicologia', v_day_of_week_psych, TIME '14:00', TIME '17:00', 30, 2, true
    WHERE NOT EXISTS (SELECT 1 FROM appointments.schedules WHERE id = v_schedule_psych_conq_id);

    INSERT INTO appointments.schedules (
        id, doctor_id, facility_id, specialty, day_of_week, start_time, end_time,
        slot_duration_minutes, max_patients_per_slot, is_active
    )
    SELECT v_schedule_occ_belen_id, v_doctor_sara_id, v_facility_belen_id, 'Terapia ocupacional', v_day_of_week_occ, TIME '13:00', TIME '15:00', 30, 2, true
    WHERE NOT EXISTS (SELECT 1 FROM appointments.schedules WHERE id = v_schedule_occ_belen_id);

    INSERT INTO appointments.schedule_blocks (
        id, schedule_id, block_date, start_time, end_time, reason
    )
    SELECT '12121212-1212-1212-1212-121212121212', v_schedule_therapy_belen_id, v_target_date_therapy, TIME '10:00', TIME '10:30', '[SEED-CENTIRBOT-TEST] Bloqueo operativo de prueba'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.schedule_blocks WHERE id = '12121212-1212-1212-1212-121212121212');

    INSERT INTO appointments.schedule_blocks (
        id, schedule_id, block_date, start_time, end_time, reason
    )
    SELECT '13131313-1313-1313-1313-131313131313', v_schedule_psych_conq_id, v_target_date_psych, TIME '16:30', TIME '17:00', '[SEED-CENTIRBOT-TEST] Bloqueo de cierre de agenda'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.schedule_blocks WHERE id = '13131313-1313-1313-1313-131313131313');

    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT '55555555-5555-5555-5555-555555555555', v_patient_ana_id, v_doctor_laura_id, v_schedule_therapy_belen_id, v_facility_belen_id, v_target_date_therapy, TIME '09:00', 30, 'PRESENCIAL', 'SCHEDULED', 'Terapia fisica', 'N8N', '[SEED-CENTIRBOT-TEST] Consulta para validar agendamiento y reprogramacion', 'Seed seguro CentirBot'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = '55555555-5555-5555-5555-555555555555');

    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT '66666666-6666-6666-6666-666666666666', v_patient_ana_id, v_doctor_laura_id, v_schedule_therapy_belen_id, v_facility_belen_id, v_target_date_therapy, TIME '09:30', 30, 'PRESENCIAL', 'CONFIRMED', 'Terapia fisica', 'N8N', '[SEED-CENTIRBOT-TEST] Cita confirmada para listado y cancelacion', 'Seed seguro CentirBot'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = '66666666-6666-6666-6666-666666666666');

    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT 'aaaaaaaa-5555-5555-5555-555555555555', v_patient_ana_id, v_doctor_mateo_id, v_schedule_psych_conq_id, v_facility_conquistadores_id, v_target_date_psych, TIME '15:00', 30, 'PRESENCIAL', 'PENDIENTE_CONFIRMACION_GRUPO', 'Psicologia', 'N8N', '[SEED-CENTIRBOT-TEST] Cita de psicologia para multiples especialidades', 'Seed seguro CentirBot'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = 'aaaaaaaa-5555-5555-5555-555555555555');

    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT 'bbbbbbbb-5555-5555-5555-555555555555', v_patient_pedro_id, v_doctor_laura_id, v_schedule_therapy_belen_id, v_facility_belen_id, v_target_date_therapy, TIME '08:00', 30, 'PRESENCIAL', 'SCHEDULED', 'Terapia fisica', 'N8N', '[SEED-CENTIRBOT-TEST] Ocupacion de cupo 1', 'Seed seguro CentirBot'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = 'bbbbbbbb-5555-5555-5555-555555555555');

    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT 'cccccccc-5555-5555-5555-555555555555', v_patient_marta_id, v_doctor_laura_id, v_schedule_therapy_belen_id, v_facility_belen_id, v_target_date_therapy, TIME '08:00', 30, 'PRESENCIAL', 'CONFIRMED', 'Terapia fisica', 'N8N', '[SEED-CENTIRBOT-TEST] Ocupacion de cupo 2', 'Seed seguro CentirBot'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = 'cccccccc-5555-5555-5555-555555555555');

    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT 'dddddddd-5555-5555-5555-555555555555', v_patient_jose_id, v_doctor_laura_id, v_schedule_therapy_belen_id, v_facility_belen_id, v_target_date_therapy, TIME '08:00', 30, 'PRESENCIAL', 'SCHEDULED', 'Terapia fisica', 'N8N', '[SEED-CENTIRBOT-TEST] Ocupacion de cupo 3', 'Seed seguro CentirBot'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = 'dddddddd-5555-5555-5555-555555555555');

    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT 'eeeeeeee-5555-5555-5555-555555555555', v_patient_luisa_id, v_doctor_mateo_id, v_schedule_psych_conq_id, v_facility_conquistadores_id, v_target_date_psych, TIME '14:00', 30, 'PRESENCIAL', 'SCHEDULED', 'Psicologia', 'N8N', '[SEED-CENTIRBOT-TEST] Cupo parcial para psicologia', 'Seed seguro CentirBot'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = 'eeeeeeee-5555-5555-5555-555555555555');

    INSERT INTO appointments.appointment_state_history (
        id, appointment_id, old_status, new_status, reason, metadata
    )
    SELECT 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', '55555555-5555-5555-5555-555555555555', NULL, 'SCHEDULED', '[SEED-CENTIRBOT-TEST] Estado inicial', jsonb_build_object('source', 'seed_centirbot_test.sql')
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointment_state_history WHERE id = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee');

    INSERT INTO appointments.appointment_state_history (
        id, appointment_id, old_status, new_status, reason, metadata
    )
    SELECT 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeef', '66666666-6666-6666-6666-666666666666', NULL, 'CONFIRMED', '[SEED-CENTIRBOT-TEST] Estado inicial', jsonb_build_object('source', 'seed_centirbot_test.sql')
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointment_state_history WHERE id = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeef');

    INSERT INTO appointments.appointment_state_history (
        id, appointment_id, old_status, new_status, reason, metadata
    )
    SELECT 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeef0', 'aaaaaaaa-5555-5555-5555-555555555555', NULL, 'PENDIENTE_CONFIRMACION_GRUPO', '[SEED-CENTIRBOT-TEST] Estado inicial', jsonb_build_object('source', 'seed_centirbot_test.sql')
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointment_state_history WHERE id = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeef0');

    INSERT INTO appointments.n8n_idempotency_keys (
        id, scope, idempotency_key, appointment_id, created_at
    )
    SELECT gen_random_uuid(), 'book_appointment', 'seed-safe-conv-001', '55555555-5555-5555-5555-555555555555', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
        SELECT 1
        FROM appointments.n8n_idempotency_keys
        WHERE scope = 'book_appointment' AND idempotency_key = 'seed-safe-conv-001'
    );

    INSERT INTO appointments.n8n_idempotency_keys (
        id, scope, idempotency_key, appointment_id, created_at
    )
    SELECT gen_random_uuid(), 'book_appointment', 'seed-safe-conv-002', '66666666-6666-6666-6666-666666666666', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
        SELECT 1
        FROM appointments.n8n_idempotency_keys
        WHERE scope = 'book_appointment' AND idempotency_key = 'seed-safe-conv-002'
    );

    INSERT INTO appointments.domain_events (
        id, event_type, aggregate_id, event_data, published
    )
    SELECT '88888888-8888-8888-8888-888888888888', 'APPOINTMENT_CREATED', '55555555-5555-5555-5555-555555555555',
        jsonb_build_object(
            'appointmentId', '55555555-5555-5555-5555-555555555555',
            'patientId', v_patient_ana_id,
            'doctorId', v_doctor_laura_id,
            'appointmentDate', v_target_date_therapy,
            'appointmentTime', TIME '09:00',
            'appointmentType', 'PRESENCIAL'
        ),
        false
    WHERE NOT EXISTS (SELECT 1 FROM appointments.domain_events WHERE id = '88888888-8888-8888-8888-888888888888');

    INSERT INTO appointments.domain_events (
        id, event_type, aggregate_id, event_data, published
    )
    SELECT '88888888-8888-8888-8888-888888888889', 'APPOINTMENT_CREATED', 'aaaaaaaa-5555-5555-5555-555555555555',
        jsonb_build_object(
            'appointmentId', 'aaaaaaaa-5555-5555-5555-555555555555',
            'patientId', v_patient_ana_id,
            'doctorId', v_doctor_mateo_id,
            'appointmentDate', v_target_date_psych,
            'appointmentTime', TIME '15:00',
            'appointmentType', 'PRESENCIAL'
        ),
        false
    WHERE NOT EXISTS (SELECT 1 FROM appointments.domain_events WHERE id = '88888888-8888-8888-8888-888888888889');

    INSERT INTO appointments.pqrs (
        id, cedula, tipo, descripcion, correo, nombres, telefono, radicado, status, metadata
    )
    SELECT '77777777-7777-7777-7777-777777777777', v_doc_ana, 'PETICION',
        '[SEED-CENTIRBOT-TEST] PQRS de prueba para verificar el formulario publico y la persistencia.',
        'centirbot.test.ana+seed@ipscentir.local', 'Ana Maria Lopez', '3200001001',
        'SEED-PQRS-' || EXTRACT(YEAR FROM CURRENT_DATE)::INT || '-000001',
        'CREADO', '{"source":"seed_centirbot_test.sql","safe":true}'
    WHERE NOT EXISTS (
        SELECT 1
        FROM appointments.pqrs
        WHERE id = '77777777-7777-7777-7777-777777777777'
           OR radicado = 'SEED-PQRS-' || EXTRACT(YEAR FROM CURRENT_DATE)::INT || '-000001'
    );
END $$;

SELECT 'Seed CentirBot seguro cargado: solo inserta datos de prueba si faltan y no pisa sedes ni pacientes reales' AS message;
