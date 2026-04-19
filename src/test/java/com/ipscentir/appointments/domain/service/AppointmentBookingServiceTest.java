package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentBookingServiceTest {

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentBookingService bookingService;

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
        date = LocalDate.now().plusDays(2);
        time = LocalTime.of(10, 0);
    }

    @Test
    void testBookAppointment_Success() {
        when(availabilityService.isSlotAvailable(doctorId, date, time)).thenReturn(true);
        when(appointmentRepository.existsByPatientIdAndDate(patientId, date)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment saved = bookingService.bookAppointment(new AppointmentBookingRequest(
            patientId, doctorId, null, scheduleId, date, time, AppointmentType.PRESENCIAL, "Routine check"
        ));

        assertNotNull(saved);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testBookAppointment_FailsIfSlotNotAvailable() {
        when(availabilityService.isSlotAvailable(doctorId, date, time)).thenReturn(false);

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, date, time, AppointmentType.PRESENCIAL, "Routine check"
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_FailsIfPatientAlreadyHasAppointmentOnDate() {
        when(availabilityService.isSlotAvailable(doctorId, date, time)).thenReturn(true);
        when(appointmentRepository.existsByPatientIdAndDate(patientId, date)).thenReturn(true);

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, date, time, AppointmentType.PRESENCIAL, "Routine check"
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_JuntaMedicaRequiresSecondSpecialist() {
        when(availabilityService.isSlotAvailable(doctorId, date, time)).thenReturn(true);

        AppointmentBookingRequest request = new AppointmentBookingRequest(
            patientId, doctorId, null, scheduleId, date, time, AppointmentType.JUNTA_MEDICA, "Junta"
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_TherapyStartsAsPendingWhenBelowMinGroup() {
        when(availabilityService.isSlotAvailable(doctorId, date, time)).thenReturn(true);
        when(appointmentRepository.existsByPatientIdAndDate(patientId, date)).thenReturn(false);
        when(appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(scheduleId, date, time, AppointmentType.TERAPIA_FISICA))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment saved = bookingService.bookAppointment(new AppointmentBookingRequest(
            patientId,
            doctorId,
            null,
            scheduleId,
            date,
            time,
            AppointmentType.TERAPIA_FISICA,
            "Terapia"
        ));

        org.junit.jupiter.api.Assertions.assertEquals(AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO, saved.getStatus());
    }
}
