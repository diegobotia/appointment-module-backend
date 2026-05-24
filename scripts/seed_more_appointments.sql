CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_patient_carlos_id UUID := '33333333-3333-3333-3333-333333333334';
    v_patient_marta_id UUID := '33333333-3333-3333-3333-333333333336';
    v_patient_jose_id UUID := '33333333-3333-3333-3333-333333333337';

    v_doctor_sara_id TEXT := 'SEED-HCMED-003';
    v_doctor_mateo_id TEXT := 'SEED-HCMED-002';
    v_doctor_laura_id TEXT := 'SEED-HCMED-001';

    v_schedule_occ_belen_id UUID := '44444444-4444-4444-4444-444444444446';
    v_schedule_psych_conq_id UUID := '44444444-4444-4444-4444-444444444445';
    v_schedule_therapy_belen_id UUID := '44444444-4444-4444-4444-444444444444';

    v_facility_belen_id UUID;
    v_facility_conquistadores_id UUID;

    -- Fechas para la próxima semana para asegurar disponibilidad
    v_target_date_1 DATE := CURRENT_DATE + 7;
    v_target_date_2 DATE := CURRENT_DATE + 8;
    v_target_date_3 DATE := CURRENT_DATE + 9;
BEGIN
    -- Obtener IDs de las sedes (asumiendo que ya existen por el seed principal)
    SELECT id INTO v_facility_conquistadores_id FROM appointments.facilities WHERE code = 'SEDE_PRINCIPAL' LIMIT 1;
    SELECT id INTO v_facility_belen_id FROM appointments.facilities WHERE code = 'SEDE_NORTE' LIMIT 1;

    -- ==========================================
    -- 1. Nueva cita para Carlos (Terapia Ocupacional con Sara en Belén)
    -- ==========================================
    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT '99999999-1111-1111-1111-111111111111', v_patient_carlos_id, v_doctor_sara_id, v_schedule_occ_belen_id, v_facility_belen_id, v_target_date_1, TIME '13:30', 30, 'PRESENCIAL', 'SCHEDULED', 'Terapia ocupacional', 'N8N', '[SEED-CENTIRBOT-EXTRA] Cita de prueba extra 1', 'Generada para pruebas adicionales'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = '99999999-1111-1111-1111-111111111111');

    INSERT INTO appointments.appointment_state_history (
        id, appointment_id, old_status, new_status, reason, metadata
    )
    SELECT '88888888-1111-1111-1111-111111111111', '99999999-1111-1111-1111-111111111111', NULL, 'SCHEDULED', '[SEED-CENTIRBOT-EXTRA] Estado inicial', jsonb_build_object('source', 'seed_more_appointments.sql')
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointment_state_history WHERE id = '88888888-1111-1111-1111-111111111111');

    INSERT INTO appointments.domain_events (
        id, event_type, aggregate_id, event_data, published
    )
    SELECT '77777777-1111-1111-1111-111111111111', 'APPOINTMENT_CREATED', '99999999-1111-1111-1111-111111111111',
        jsonb_build_object('appointmentId', '99999999-1111-1111-1111-111111111111', 'patientId', v_patient_carlos_id, 'doctorId', v_doctor_sara_id, 'appointmentDate', v_target_date_1, 'appointmentTime', TIME '13:30', 'appointmentType', 'PRESENCIAL'), false
    WHERE NOT EXISTS (SELECT 1 FROM appointments.domain_events WHERE id = '77777777-1111-1111-1111-111111111111');

    -- ==========================================
    -- 2. Nueva cita para Marta (Psicología con Mateo en Conquistadores)
    -- ==========================================
    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT '99999999-2222-2222-2222-222222222222', v_patient_marta_id, v_doctor_mateo_id, v_schedule_psych_conq_id, v_facility_conquistadores_id, v_target_date_2, TIME '14:30', 30, 'VIRTUAL', 'CONFIRMED', 'Psicologia', 'N8N', '[SEED-CENTIRBOT-EXTRA] Cita de prueba extra 2 (Virtual)', 'Generada para pruebas adicionales'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = '99999999-2222-2222-2222-222222222222');

    INSERT INTO appointments.appointment_state_history (
        id, appointment_id, old_status, new_status, reason, metadata
    )
    SELECT '88888888-2222-2222-2222-222222222222', '99999999-2222-2222-2222-222222222222', NULL, 'CONFIRMED', '[SEED-CENTIRBOT-EXTRA] Estado inicial', jsonb_build_object('source', 'seed_more_appointments.sql')
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointment_state_history WHERE id = '88888888-2222-2222-2222-222222222222');

    INSERT INTO appointments.domain_events (
        id, event_type, aggregate_id, event_data, published
    )
    SELECT '77777777-2222-2222-2222-222222222222', 'APPOINTMENT_CREATED', '99999999-2222-2222-2222-222222222222',
        jsonb_build_object('appointmentId', '99999999-2222-2222-2222-222222222222', 'patientId', v_patient_marta_id, 'doctorId', v_doctor_mateo_id, 'appointmentDate', v_target_date_2, 'appointmentTime', TIME '14:30', 'appointmentType', 'VIRTUAL'), false
    WHERE NOT EXISTS (SELECT 1 FROM appointments.domain_events WHERE id = '77777777-2222-2222-2222-222222222222');

    -- ==========================================
    -- 3. Nueva cita para José (Terapia Física con Laura en Belén)
    -- ==========================================
    INSERT INTO appointments.appointments (
        id, patient_id, doctor_id, schedule_id, facility_id, appointment_date, appointment_time,
        duration_minutes, appointment_type, status, specialty, booking_channel, reason, notes
    )
    SELECT '99999999-3333-3333-3333-333333333333', v_patient_jose_id, v_doctor_laura_id, v_schedule_therapy_belen_id, v_facility_belen_id, v_target_date_3, TIME '10:00', 30, 'PRESENCIAL', 'PENDIENTE_CONFIRMACION_GRUPO', 'Terapia fisica', 'N8N', '[SEED-CENTIRBOT-EXTRA] Cita de prueba extra 3', 'Generada para pruebas adicionales'
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointments WHERE id = '99999999-3333-3333-3333-333333333333');

    INSERT INTO appointments.appointment_state_history (
        id, appointment_id, old_status, new_status, reason, metadata
    )
    SELECT '88888888-3333-3333-3333-333333333333', '99999999-3333-3333-3333-333333333333', NULL, 'PENDIENTE_CONFIRMACION_GRUPO', '[SEED-CENTIRBOT-EXTRA] Estado inicial', jsonb_build_object('source', 'seed_more_appointments.sql')
    WHERE NOT EXISTS (SELECT 1 FROM appointments.appointment_state_history WHERE id = '88888888-3333-3333-3333-333333333333');

    INSERT INTO appointments.domain_events (
        id, event_type, aggregate_id, event_data, published
    )
    SELECT '77777777-3333-3333-3333-333333333333', 'APPOINTMENT_CREATED', '99999999-3333-3333-3333-333333333333',
        jsonb_build_object('appointmentId', '99999999-3333-3333-3333-333333333333', 'patientId', v_patient_jose_id, 'doctorId', v_doctor_laura_id, 'appointmentDate', v_target_date_3, 'appointmentTime', TIME '10:00', 'appointmentType', 'PRESENCIAL'), false
    WHERE NOT EXISTS (SELECT 1 FROM appointments.domain_events WHERE id = '77777777-3333-3333-3333-333333333333');

END $$;

SELECT 'Citas adicionales de prueba insertadas exitosamente.' AS message;
