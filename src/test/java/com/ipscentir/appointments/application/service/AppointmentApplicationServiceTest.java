package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentApplicationServiceTest {

    @Mock
    private AppointmentBookingService bookingService;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentApplicationService applicationService;

    private UUID patientId;
    private UUID doctorId;
    private UUID scheduleId;
    private LocalDate date;
    private LocalTime time;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        date = LocalDate.now().plusDays(3);
        time = LocalTime.of(9, 30);
    }

    @Test
    void testCreateAppointment_MapsCommandToDtoSuccessfully() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                patientId, doctorId, null, scheduleId, date, time, "TELEMEDICINA", "Symptoms"
        );

        Appointment appointment = Appointment.scheduleNew(
                patientId, doctorId, null, scheduleId, date, time, 30, AppointmentType.TELEMEDICINA, AppointmentStatus.SCHEDULED, "Symptoms"
        );

        AppointmentDTO mappedDto = new AppointmentDTO(
                appointment.getId(), patientId, doctorId, null, scheduleId, date, time, 30,
                AppointmentType.TELEMEDICINA, AppointmentStatus.SCHEDULED, "Symptoms", null, null, null
        );

        when(bookingService.bookAppointment(new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, date, time, AppointmentType.TELEMEDICINA, "Symptoms"
        ))).thenReturn(appointment);

        when(appointmentMapper.toDto(appointment)).thenReturn(mappedDto);

        AppointmentDTO result = applicationService.createAppointment(command);

        assertNotNull(result);
        assertEquals(AppointmentType.TELEMEDICINA, result.appointmentType());
        verify(bookingService).bookAppointment(new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, date, time, AppointmentType.TELEMEDICINA, "Symptoms"
        ));
        verify(appointmentMapper).toDto(appointment);
    }
}
