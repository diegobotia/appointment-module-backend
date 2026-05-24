package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentBookingServiceTest {

    @Mock
    private HumanResourceAvailabilityService humanResourceAvailabilityService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ResourceCapacityService resourceCapacityService;

    @InjectMocks
    private AppointmentBookingService bookingService;

    private UUID patientId;
    private String doctorId;
    private Integer sedeId;
    private UUID scheduleId;
    private LocalDate date;
    private LocalTime time;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID().toString();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        scheduleId = UUID.randomUUID();
        date = LocalDate.now().plusDays(2);
        time = LocalTime.of(10, 0);
    }

    @Test
    void testBookAppointment_Success() {
        doNothing().when(humanResourceAvailabilityService).assertBookingAllowed(any());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment saved = bookingService.bookAppointment(new AppointmentBookingRequest(
                patientId,
                doctorId,
                null,
                scheduleId,
                sedeId,
                date,
                time,
                AppointmentType.PRESENCIAL,
                "Routine check",
                null,
                null
        ));

        assertNotNull(saved);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(resourceCapacityService).allocate(saved);
    }

    @Test
    void testBookAppointment_FailsIfSlotNotAvailable() {
        doThrow(new IllegalStateException("The requested slot is not available for this doctor."))
                .when(humanResourceAvailabilityService).assertBookingAllowed(any());

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, sedeId, date, time,
                AppointmentType.PRESENCIAL, "Routine check", null, null
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_FailsIfPatientAlreadyHasAppointmentOnDate() {
        doThrow(new IllegalStateException("The patient already has an active appointment for this date."))
                .when(humanResourceAvailabilityService).assertBookingAllowed(any());

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, sedeId, date, time,
                AppointmentType.PRESENCIAL, "Routine check", null, null
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_JuntaMedicaRequiresSecondSpecialist() {
        doThrow(new IllegalStateException("Junta medica requires exactly 2 specialists"))
                .when(humanResourceAvailabilityService).assertBookingAllowed(any());

        AppointmentBookingRequest request = new AppointmentBookingRequest(
                patientId, doctorId, null, scheduleId, sedeId, date, time,
                AppointmentType.JUNTA_MEDICA, "Junta", null, null
        );

        assertThrows(IllegalStateException.class, () -> bookingService.bookAppointment(request));
    }

    @Test
    void testBookAppointment_TherapyStartsAsPendingWhenBelowMinGroup() {
        doNothing().when(humanResourceAvailabilityService).assertBookingAllowed(any());
        doNothing().when(humanResourceAvailabilityService).assertTherapyGroupAllowsNewPatient(
                scheduleId, date, time, AppointmentType.TERAPIA_FISICA
        );
        when(appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(
                scheduleId, date, time, AppointmentType.TERAPIA_FISICA
        )).thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment saved = bookingService.bookAppointment(new AppointmentBookingRequest(
                patientId,
                doctorId,
                null,
                scheduleId,
                sedeId,
                date,
                time,
                AppointmentType.TERAPIA_FISICA,
                "Terapia",
                null,
                null
        ));

        assertEquals(AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO, saved.getStatus());
    }

    @Test
    void bookAdministrativeAppointmentPersistsStaffMeetingWithoutPatient() {
        List<String> participants = List.of(doctorId, UUID.randomUUID().toString());
        doNothing().when(humanResourceAvailabilityService).assertAdministrativeBookingAllowed(
                participants, sedeId, date, time, 45
        );
        doNothing().when(resourceCapacityService).assertCanAllocate(
                sedeId, AppointmentType.STAFF, null, date, time, 45, null
        );
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment saved = bookingService.bookAdministrativeAppointment(
                new AdministrativeAppointmentBookingRequest(participants, sedeId, date, time, 45, "Bloqueo")
        );

        assertNull(saved.getPatientId());
        assertEquals(AppointmentType.STAFF, saved.getAppointmentType());
        verify(resourceCapacityService).allocate(saved);
    }
}
