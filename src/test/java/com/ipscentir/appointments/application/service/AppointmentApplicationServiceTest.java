package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAdministrativeAppointmentCommand;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.security.SedeAuthorizationService;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.service.AdministrativeAppointmentBookingRequest;
import com.ipscentir.appointments.domain.service.AppointmentBookingRequest;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentApplicationServiceTest {

    @Mock
    private AppointmentBookingService bookingService;

    @Mock
    private AppointmentEnrichmentService appointmentEnrichmentService;

    @Mock
    private SedeAuthorizationService sedeAuthorizationService;

    @Mock
    private com.ipscentir.appointments.infrastructure.observability.AppointmentsMetrics appointmentsMetrics;

    @InjectMocks
    private AppointmentApplicationService applicationService;

    private UUID patientId;
    private String doctorId;
    private UUID scheduleId;
    private LocalDate date;
    private LocalTime time;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = java.util.UUID.randomUUID().toString();
        scheduleId = UUID.randomUUID();
        date = LocalDate.now().plusDays(3);
        time = LocalTime.of(9, 30);
    }

    @Test
    void testCreateAppointment_MapsCommandToDtoSuccessfully() {
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                patientId, doctorId, sedeId, null, scheduleId, date, time, "PRESENCIAL", "Symptoms", null, null
        );

        Appointment appointment = Appointment.scheduleNew(
                patientId, doctorId.toString(), null, new AppointmentScheduleData(scheduleId, sedeId, date, time, 30, AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, "Symptoms")
        );

        AppointmentDTO mappedDto = new AppointmentDTO(
                appointment.getId(), patientId, doctorId, sedeId, null, scheduleId, date, time, 30,
                AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, BookingChannel.STAFF, null, "Symptoms", null, null, null,
                null, null, false
        );

        when(bookingService.bookAppointment(new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, sedeId, date, time, AppointmentType.PRESENCIAL, "Symptoms", BookingChannel.STAFF, null
        ))).thenReturn(appointment);

        when(appointmentEnrichmentService.toDto(appointment)).thenReturn(mappedDto);

        AppointmentDTO result = applicationService.createAppointment(command);

        assertNotNull(result);
        assertEquals(AppointmentType.PRESENCIAL, result.appointmentType());
        verify(sedeAuthorizationService).assertCurrentUserCanAccessSede(sedeId);
        verify(bookingService).bookAppointment(new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, sedeId, date, time, AppointmentType.PRESENCIAL, "Symptoms", BookingChannel.STAFF, null
        ));
        verify(appointmentEnrichmentService).toDto(appointment);
    }

    @Test
    void createAdministrativeAppointmentBooksWithoutPatient() {
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        List<String> participants = List.of(doctorId, UUID.randomUUID().toString());
        CreateAdministrativeAppointmentCommand command = new CreateAdministrativeAppointmentCommand(
                participants, sedeId, date, time, 60, "Junta operativa"
        );

        Appointment appointment = Appointment.scheduleStaffMeeting(
                participants,
                new AppointmentScheduleData(null, sedeId, date, time, 60, AppointmentType.STAFF, AppointmentStatus.SCHEDULED, "Junta operativa"),
                BookingChannel.STAFF
        );

        AppointmentDTO mappedDto = new AppointmentDTO(
                appointment.getId(), null, doctorId, sedeId, participants.get(1), null, date, time, 60,
                AppointmentType.STAFF, AppointmentStatus.SCHEDULED, BookingChannel.STAFF, null, "Junta operativa", null, null, null,
                null, null, true
        );

        when(bookingService.bookAdministrativeAppointment(any())).thenReturn(appointment);
        when(appointmentEnrichmentService.toDto(appointment)).thenReturn(mappedDto);

        AppointmentDTO result = applicationService.createAdministrativeAppointment(command);

        assertNull(result.patientId());
        assertEquals(AppointmentType.STAFF, result.appointmentType());
        verify(sedeAuthorizationService).assertCurrentUserCanAccessSede(sedeId);
        verify(bookingService).bookAdministrativeAppointment(new AdministrativeAppointmentBookingRequest(
                participants, sedeId, date, time, 60, "Junta operativa"
        ));
    }

    @Test
    void createAdministrativeAppointmentRejectsDuplicateParticipants() {
        CreateAdministrativeAppointmentCommand command = new CreateAdministrativeAppointmentCommand(
                List.of(doctorId, doctorId),
                FacilityMasterData.SEDE_ID_BELEN,
                date,
                time,
                30,
                "Duplicado"
        );

        assertThrows(IllegalArgumentException.class, () -> applicationService.createAdministrativeAppointment(command));
    }
}
