package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nFacilityId;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientIdentifyRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventRequest;
import com.ipscentir.appointments.application.exception.PatientNotFoundException;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlotDetail;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.domain.service.AvailabilityService;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class N8nPatientIntegrationServiceTest {

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private AppointmentApplicationService appointmentApplicationService;

    @Mock
    private AppointmentBookingService appointmentBookingService;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private N8nEventJournalService n8nEventJournalService;

    @Mock
    private PatientRegistrationService patientRegistrationService;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private SedeLookupService sedeLookupService;

    @Mock
    private N8nIdempotencyService n8nIdempotencyService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SpecialistJpaRepository specialistJpaRepository;

    @Mock
    private AppointmentOperationsService appointmentOperationsService;

    @Mock
    private TipoIdentificacionResolver tipoIdentificacionResolver;

    @Mock
    private MedicoLookupService medicoLookupService;

    @InjectMocks
    private N8nPatientIntegrationService service;

    @Test
    void shouldReturnAvailabilitySummary() {
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        LocalDate date = LocalDate.now().plusDays(2);

        when(availabilityService.getNearestAvailableSlotsByServiceType(AppointmentServiceType.TERAPIA_FISICA, sedeId, date, 4)).thenReturn(List.of(
                new AvailableSlotDetail(UUID.randomUUID(), UUID.randomUUID().toString(), FacilityMasterData.SEDE_ID_BELEN, AppointmentServiceType.TERAPIA_FISICA, "Terapia fisica", date, LocalTime.of(8, 0), 30, 3)
        ));

        var response = service.getAvailability(new N8nPatientAvailabilityRequest("TERAPIA_FISICA", null, N8nFacilityId.BELEN, 4, date));

        assertEquals(1, response.availableSlotsCount());
        assertEquals("Se encontraron 1 horarios disponibles para Terapia fisica.", response.summary());
    }

    @Test
    void shouldIdentifyExistingPatient() {
        UUID patientId = UUID.randomUUID();
        Paciente paciente = Paciente.builder()
                .id(patientId)
                .nombres("Ana")
                .apellidos("Lopez")
                .codTipoIdentificacion("CC")
                .numIdentificacion("123")
                .build();

        when(tipoIdentificacionResolver.resolveCodigo("CC")).thenReturn("13");
        when(tipoIdentificacionResolver.resolveDescripcion("CC")).thenReturn("Cédula de ciudadanía");
        when(tipoIdentificacionResolver.findPaciente("CC", "123")).thenReturn(Optional.of(paciente));
        when(patientRegistrationService.getFormConfig()).thenReturn(
                new com.ipscentir.appointments.application.dto.form.PatientRegistrationFormConfigResponse(
                        "https://form",
                        "/submit",
                        "/status",
                        List.of(new com.ipscentir.appointments.application.dto.form.DocumentTypeOptionDTO("13", "Cédula de ciudadanía")),
                        "https://form?codTipoIdentificacion={codTipoIdentificacion}&numIdentificacion={numIdentificacion}"
                )
        );

        var response = service.identifyPatient(new N8nPatientIdentifyRequest("CC", "123"));

        assertTrue(response.found());
        assertEquals(patientId, response.patientId());
        assertEquals("Cédula de ciudadanía", response.tipoIdentificacion());
    }

    @Test
    void shouldListAppointmentsWithSpecialtyAndFacilityCode() {
        UUID patientId = UUID.randomUUID();
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        UUID scheduleId = UUID.randomUUID();
        Paciente paciente = Paciente.builder()
                .id(patientId)
                .codTipoIdentificacion("CC")
                .numIdentificacion("123")
                .build();

        Appointment appointment = Appointment.scheduleNew(
                patientId,
                UUID.randomUUID().toString(),
                null,
                new AppointmentScheduleData(scheduleId, sedeId, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30, AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, "Control")
        );

        when(tipoIdentificacionResolver.findPaciente("CC", "123")).thenReturn(Optional.of(paciente));
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId))
                .thenReturn(List.of(appointment));
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(
                Schedule.builder().id(scheduleId).doctorId(appointment.getDoctorId()).sedeId(sedeId).specialty("Medico fisiatria").build()
        ));

        var response = service.listAppointmentsByDocument("CC", "123");

        assertEquals(1, response.appointments().size());
        var summary = response.appointments().getFirst();
        assertEquals("BELEN", summary.facilityCode());
        assertEquals(scheduleId, summary.scheduleId());
        assertEquals("Medico fisiatria", summary.specialty());
        assertEquals("MEDICO_FISIATRIA", summary.serviceType());
    }

    @Test
    void shouldFallbackToDoctorSpecialtyWhenScheduleIsMissing() {
        UUID patientId = UUID.randomUUID();
        Integer sedeId = FacilityMasterData.SEDE_ID_CONQUISTADORES;
        String doctorId = UUID.randomUUID().toString();
        Paciente paciente = Paciente.builder()
                .id(patientId)
                .codTipoIdentificacion("CC")
                .numIdentificacion("456")
                .build();

        Appointment appointment = Appointment.scheduleNew(
                patientId,
                doctorId,
                null,
                new AppointmentScheduleData(null, sedeId, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30, AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, "Control")
        );

        when(tipoIdentificacionResolver.findPaciente("CC", "456")).thenReturn(Optional.of(paciente));
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId))
                .thenReturn(List.of(appointment));
        when(medicoLookupService.findPrimarySpecialty(doctorId)).thenReturn(Optional.of("Psicologia"));

        var response = service.listAppointmentsByDocument("CC", "456");

        var summary = response.appointments().getFirst();
        assertEquals("Psicologia", summary.specialty());
        assertEquals("PSICOLOGIA", summary.serviceType());
        assertEquals("CONQUISTADORES", summary.facilityCode());
    }

    @Test
    void shouldReturnNullServicePairWhenTextDoesNotMatchCatalog() {
        UUID patientId = UUID.randomUUID();
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        String doctorId = UUID.randomUUID().toString();
        Paciente paciente = Paciente.builder()
                .id(patientId)
                .codTipoIdentificacion("CC")
                .numIdentificacion("789")
                .build();

        Appointment appointment = Appointment.scheduleNew(
                patientId,
                doctorId,
                null,
                new AppointmentScheduleData(null, sedeId, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30, AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, "Control")
        );

        when(tipoIdentificacionResolver.findPaciente("CC", "789")).thenReturn(Optional.of(paciente));
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId))
                .thenReturn(List.of(appointment));
        when(medicoLookupService.findPrimarySpecialty(doctorId)).thenReturn(Optional.of("Ortopedia general"));

        var summary = service.listAppointmentsByDocument("CC", "789").appointments().getFirst();

        assertEquals(null, summary.serviceType());
        assertEquals(null, summary.specialty());
    }

    @Test
    void shouldRejectBookingWhenPatientDoesNotExist() {
        UUID patientId = UUID.randomUUID();
        when(pacienteRepository.existsById(patientId)).thenReturn(false);

        assertThrows(PatientNotFoundException.class, () -> service.createAppointment(new N8nPatientAppointmentRequest(
                patientId,
                UUID.randomUUID().toString(),
                N8nFacilityId.BELEN,
                null,
                UUID.randomUUID(),
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                "Checkup",
                null,
                null
        )));
    }

    @Test
    void shouldReturnCachedAppointmentForIdempotencyKey() {
        UUID patientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        String doctorId = UUID.randomUUID().toString();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        Appointment appointment = Appointment.scheduleNew(
                patientId,
                doctorId,
                null,
                new AppointmentScheduleData(UUID.randomUUID(), FacilityMasterData.SEDE_ID_BELEN, date, time, 30, AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, "Checkup")
        );

        when(pacienteRepository.existsById(patientId)).thenReturn(true);
        when(n8nIdempotencyService.findAppointmentId(N8nIdempotencyService.SCOPE_BOOK_APPOINTMENT, "conv-1"))
                .thenReturn(Optional.of(appointmentId));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentMapper.toDto(appointment)).thenReturn(new AppointmentDTO(
                appointmentId, patientId, doctorId, FacilityMasterData.SEDE_ID_BELEN, null, UUID.randomUUID(), date, time, 30,
                AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, BookingChannel.STAFF, null, "Checkup", null, null, null,
                null, null, false
        ));

        var response = service.createAppointment(new N8nPatientAppointmentRequest(
                patientId, doctorId, N8nFacilityId.BELEN, null, UUID.randomUUID(), date, time, "Checkup", "conv-1", null
        ));

        assertEquals(appointmentId, response.appointment().id());
    }

    @Test
    void shouldMapCreatedAppointmentResponse() {
        UUID patientId = UUID.randomUUID();
        String doctorId = UUID.randomUUID().toString();
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        UUID scheduleId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        AppointmentDTO dto = new AppointmentDTO(
                UUID.randomUUID(), patientId, doctorId, sedeId, null, scheduleId, date, time, 30,
                AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, BookingChannel.N8N, null, "Checkup", null, null, null,
                null, null, false
        );

        when(pacienteRepository.existsById(patientId)).thenReturn(true);
        when(n8nIdempotencyService.findAppointmentId(any(), any())).thenReturn(Optional.empty());
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(
                Schedule.builder().id(scheduleId).specialty("Medico laboral").build()
        ));
        when(appointmentApplicationService.createAppointment(any())).thenReturn(dto);

        var response = service.createAppointment(new N8nPatientAppointmentRequest(
                patientId, doctorId, N8nFacilityId.BELEN, null, scheduleId, date, time, "Checkup", null, "req-1"
        ));

        assertEquals(dto, response.appointment());
        verify(n8nIdempotencyService).storeAppointmentBooking("req-1", dto.id());
    }

    @Test
    void shouldCancelAppointment() {
        UUID appointmentId = UUID.randomUUID();
        Appointment cancelled = Appointment.scheduleNew(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                null,
                new AppointmentScheduleData(UUID.randomUUID(), FacilityMasterData.SEDE_ID_BELEN, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30, AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, "Checkup")
        );

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(cancelled));
        when(appointmentBookingService.cancelAppointment(appointmentId, "Cambio de plan")).thenReturn(cancelled);
        when(appointmentMapper.toDto(cancelled)).thenReturn(new AppointmentDTO(
                cancelled.getId(), cancelled.getPatientId(), cancelled.getDoctorId(), cancelled.getSedeId(), null,
                cancelled.getScheduleId(), cancelled.getAppointmentDate(), cancelled.getAppointmentTime(), 30,
                cancelled.getAppointmentType(), cancelled.getStatus(), BookingChannel.STAFF, null, cancelled.getReason(), null, null, null,
                null, null, false
        ));

        var response = service.cancelAppointment(appointmentId, new N8nCancelAppointmentRequest("Cambio de plan", null));

        assertEquals("Cita cancelada correctamente desde el flujo n8n.", response.summary());
    }

    @Test
    void shouldStoreWebhookEvent() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(n8nEventJournalService.handleWebhookEvent(any())).thenReturn(
                new com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventResponse(
                        eventId, "APPOINTMENT_CREATED", aggregateId, false, "RECEIVED", "Event stored successfully", java.time.LocalDateTime.now()
                )
        );

        var response = service.handleWebhookEvent(new N8nWebhookEventRequest(
                eventId, "APPOINTMENT_CREATED", aggregateId, "n8n", Map.of("foo", "bar"), false
        ));

        assertEquals("APPOINTMENT_CREATED", response.eventType());
        assertNotNull(response.aggregateId());
    }
}
