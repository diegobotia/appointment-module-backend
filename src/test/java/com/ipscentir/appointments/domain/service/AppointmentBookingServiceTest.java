package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
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

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private AppointmentBookingService bookingService;

    private UUID patientId;
    private UUID doctorId;
    private UUID facilityId;
    private UUID scheduleId;
    private LocalDate date;
    private LocalTime time;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        date = LocalDate.now().plusDays(2);
        time = LocalTime.of(10, 0);

        Schedule schedule = Schedule.builder()
                .id(scheduleId)
                .doctorId(doctorId)
                .facilityId(facilityId)
                .dayOfWeek(date.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(6)
                .isActive(true)
                .build();

        when(scheduleRepository.findById(scheduleId)).thenReturn(java.util.Optional.of(schedule));
    }

    @Test
    void testBookAppointment_Success() {
        when(availabilityService.isSlotAvailable(doctorId, facilityId, date, time)).thenReturn(true);
        when(appointmentRepository.existsByPatientIdAndDate(patientId, date)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment saved = bookingService.bookAppointment(new AppointmentBookingRequest(
                patientId,
                doctorId,
                null,
                scheduleId,
                facilityId,
                date,
                time,
                AppointmentType.PRESENCIAL,
                "Routine check"
        ));

        assertNotNull(saved);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testBookAppointment_FailsIfSlotNotAvailable() {
        when(availabilityService.isSlotAvailable(doctorId, facilityId, date, time)).thenReturn(false);

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, facilityId, date, time, AppointmentType.PRESENCIAL, "Routine check"
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_FailsIfPatientAlreadyHasAppointmentOnDate() {
        when(availabilityService.isSlotAvailable(doctorId, facilityId, date, time)).thenReturn(true);
        when(appointmentRepository.existsByPatientIdAndDate(patientId, date)).thenReturn(true);

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, facilityId, date, time, AppointmentType.PRESENCIAL, "Routine check"
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_JuntaMedicaRequiresSecondSpecialist() {
        when(availabilityService.isSlotAvailable(doctorId, facilityId, date, time)).thenReturn(true);

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, facilityId, date, time, AppointmentType.JUNTA_MEDICA, "Junta"
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_TherapyStartsAsPendingWhenBelowMinGroup() {
        when(availabilityService.isSlotAvailable(doctorId, facilityId, date, time)).thenReturn(true);
        when(appointmentRepository.existsByPatientIdAndDate(patientId, date)).thenReturn(false);
        when(appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(scheduleId, date, time, AppointmentType.TERAPIA_FISICA))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment saved = bookingService.bookAppointment(new AppointmentBookingRequest(
                patientId,
                doctorId,
                null,
                scheduleId,
                facilityId,
                date,
                time,
                AppointmentType.TERAPIA_FISICA,
                "Terapia"
        ));

        org.junit.jupiter.api.Assertions.assertEquals(AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO, saved.getStatus());
    }
}
