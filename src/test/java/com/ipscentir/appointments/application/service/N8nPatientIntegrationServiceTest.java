package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventRequest;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlot;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.domain.service.AvailabilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private N8nEventJournalService n8nEventJournalService;

    @InjectMocks
    private N8nPatientIntegrationService service;

    @Test
    void shouldReturnAvailabilitySummary() {
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);

        when(availabilityService.getAvailableSlots(doctorId, date)).thenReturn(List.of(
                new AvailableSlot(date, LocalTime.of(8, 0), 30),
                new AvailableSlot(date, LocalTime.of(8, 30), 30)
        ));

        var response = service.getAvailability(new N8nPatientAvailabilityRequest(doctorId, date));

        assertEquals(2, response.availableSlotsCount());
        assertEquals(2, response.slots().size());
        assertEquals("Se encontraron 2 horarios disponibles.", response.summary());
    }

    @Test
    void shouldMapCreatedAppointmentResponse() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        Appointment appointment = Appointment.scheduleNew(
                patientId,
                doctorId,
                null,
                scheduleId,
                date,
                time,
                30,
                AppointmentType.PRESENCIAL,
                AppointmentStatus.SCHEDULED,
                "Checkup"
        );

        AppointmentDTO dto = new AppointmentDTO(
                appointment.getId(), patientId, doctorId, null, scheduleId, date, time, 30,
                AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, "Checkup", null, null, null
        );

        when(appointmentApplicationService.createAppointment(any())).thenReturn(dto);

        var response = service.createAppointment(new N8nPatientAppointmentRequest(
                patientId, doctorId, null, scheduleId, date, time, "PRESENCIAL", "Checkup"
        ));

        assertEquals(dto, response.appointment());
        assertNotNull(response.summary());
    }

    @Test
    void shouldCancelAppointment() {
        UUID appointmentId = UUID.randomUUID();
        Appointment cancelled = Appointment.scheduleNew(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                30,
                AppointmentType.PRESENCIAL,
                AppointmentStatus.SCHEDULED,
                "Checkup"
        );

        when(appointmentBookingService.cancelAppointment(appointmentId, "Cambio de plan")).thenReturn(cancelled);
        when(appointmentMapper.toDto(cancelled)).thenReturn(new AppointmentDTO(
                cancelled.getId(), cancelled.getPatientId(), cancelled.getDoctorId(), null,
                cancelled.getScheduleId(), cancelled.getAppointmentDate(), cancelled.getAppointmentTime(), 30,
                cancelled.getAppointmentType(), cancelled.getStatus(), cancelled.getReason(), null, null, null
        ));

        var response = service.cancelAppointment(appointmentId, new N8nCancelAppointmentRequest("Cambio de plan"));

        assertEquals("Cita cancelada correctamente desde el flujo n8n.", response.summary());
        verify(appointmentBookingService).cancelAppointment(appointmentId, "Cambio de plan");
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
                eventId,
                "APPOINTMENT_CREATED",
                aggregateId,
                "n8n",
                Map.of("foo", "bar"),
                false
        ));

        assertEquals("APPOINTMENT_CREATED", response.eventType());
        assertEquals(aggregateId, response.aggregateId());
    }
}