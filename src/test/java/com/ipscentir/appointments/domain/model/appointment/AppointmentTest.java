package com.ipscentir.appointments.domain.model.appointment;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentTest {

    private Appointment appointment;
    private UUID patientId;
    private String doctorId;
    private Integer sedeId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = java.util.UUID.randomUUID().toString();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        scheduleId = UUID.randomUUID();

        appointment = Appointment.scheduleNew(
                patientId,
            doctorId.toString(),
                null,
                new AppointmentScheduleData(
                        scheduleId,
                        sedeId,
                        LocalDate.now().plusDays(2),
                        LocalTime.of(10, 0),
                        30,
                        AppointmentType.PRESENCIAL,
                        AppointmentStatus.SCHEDULED,
                        "Checkup"
                )
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void testScheduleNew_InitializesCorrectly_AndFiresEvent() {
        assertNotNull(appointment);
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
        assertEquals(AppointmentType.PRESENCIAL, appointment.getAppointmentType());

        var events = (java.util.Collection<Object>) org.springframework.test.util.ReflectionTestUtils.invokeMethod(appointment, "domainEvents");
        assertEquals(1, events.size());
        assertTrue(events.iterator().next() instanceof AppointmentCreatedEvent);
    }

    @Test
    void testConfirm_ChangesStatusToConfirmed() {
        appointment.confirm();
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertNotNull(appointment.getConfirmedAt());
    }

    @Test
    void testConfirm_FailsIfAlreadyCancelled() {
        appointment.cancel("Changed mind");

        IllegalStateException ex = assertThrows(IllegalStateException.class, appointment::confirm);
        assertEquals("Only SCHEDULED or PENDIENTE_CONFIRMACION_GRUPO appointments can be confirmed", ex.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCancel_ChangesStatusToCancelled_AndFiresEvent() {
        appointment.cancel("Sick");

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals("Sick", appointment.getCancellationReason());
        assertNotNull(appointment.getCancelledAt());

        var events = (java.util.Collection<Object>) org.springframework.test.util.ReflectionTestUtils.invokeMethod(appointment, "domainEvents");
        boolean hasCancelEvent = events.stream().anyMatch(AppointmentCancelledEvent.class::isInstance);
        assertTrue(hasCancelEvent);
    }

    @Test
    void testCheckIn_FromScheduled() {
        appointment.confirm();
        appointment.checkIn();
        assertEquals(AppointmentStatus.CHECKED_IN, appointment.getStatus());
    }

    @Test
    void testMarkNoShow_FromConfirmed() {
        appointment.confirm();
        appointment.markNoShow();
        assertEquals(AppointmentStatus.NO_SHOW, appointment.getStatus());
    }

    @Test
    void testComplete_FromCheckedIn() {
        appointment.confirm();
        appointment.checkIn();
        appointment.complete();
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }

    @Test
    void testReschedule_UpdatesScheduleAndResetsConfirmed() {
        appointment.confirm();
        LocalDate newDate = LocalDate.now().plusDays(5);
        UUID newSchedule = UUID.randomUUID();
        Integer newSedeId = FacilityMasterData.SEDE_ID_BELEN;
        String newDoctor = UUID.randomUUID().toString();

        appointment.reschedule(newDate, LocalTime.of(11, 0), newSchedule, newDoctor, newSedeId, BookingChannel.STAFF, null);

        assertEquals(newDate, appointment.getAppointmentDate());
        assertEquals(LocalTime.of(11, 0), appointment.getAppointmentTime());
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @Test
    void scheduleStaffMeetingCreatesAdministrativeAppointmentWithoutPatient() {
        String primary = UUID.randomUUID().toString();
        String secondary = UUID.randomUUID().toString();

        Appointment staffMeeting = Appointment.scheduleStaffMeeting(
                List.of(primary, secondary),
                new AppointmentScheduleData(
                        null,
                        sedeId,
                        LocalDate.now().plusDays(3),
                        LocalTime.of(14, 0),
                        60,
                        AppointmentType.STAFF,
                        AppointmentStatus.SCHEDULED,
                        "Junta interna"
                ),
                BookingChannel.STAFF
        );

        assertNull(staffMeeting.getPatientId());
        assertEquals(AppointmentType.STAFF, staffMeeting.getAppointmentType());
        assertTrue(staffMeeting.isAdministrative());
        assertEquals(primary, staffMeeting.getDoctorId());
        assertEquals(secondary, staffMeeting.getSecondaryDoctorId());
    }

    @Test
    void checkInRejectsAdministrativeAppointment() {
        Appointment administrative = Appointment.scheduleStaffMeeting(
                List.of(doctorId),
                new AppointmentScheduleData(
                        null,
                        sedeId,
                        LocalDate.now().plusDays(3),
                        LocalTime.of(15, 0),
                        30,
                        AppointmentType.STAFF,
                        AppointmentStatus.SCHEDULED,
                        "Bloqueo"
                ),
                BookingChannel.STAFF
        );

        assertThrows(IllegalStateException.class, administrative::checkIn);
    }

    @Test
    void testCancel_FailsIfAppointmentInThePast() {
        Appointment pastAppointment = Appointment.builder()
                .patientId(patientId)
            .doctorId(doctorId.toString())
                .sedeId(sedeId)
                .appointmentDate(LocalDate.now().minusDays(1))
                .appointmentTime(LocalTime.of(8, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> pastAppointment.cancel("Too late"));
        assertEquals("Cannot cancel an appointment in the past", ex.getMessage());
    }
}
