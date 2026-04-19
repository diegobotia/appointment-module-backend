package com.ipscentir.appointments.domain.model.appointment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentTest {

    private Appointment appointment;
    private UUID patientId;
    private UUID doctorId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        
        appointment = Appointment.scheduleNew(
                patientId,
                doctorId,
            null,
                scheduleId,
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                30,
                AppointmentType.PRESENCIAL,
            AppointmentStatus.SCHEDULED,
                "Checkup"
        );
    }

    @Test
    void testScheduleNew_InitializesCorrectly_AndFiresEvent() {
        assertNotNull(appointment);
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
        assertEquals(AppointmentType.PRESENCIAL, appointment.getAppointmentType());
        
        // As it extends AbstractAggregateRoot, it holds domain events eagerly before save
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
    void testCancel_ChangesStatusToCancelled_AndFiresEvent() {
        appointment.cancel("Sick");
        
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals("Sick", appointment.getCancellationReason());
        assertNotNull(appointment.getCancelledAt());
        
        var events = (java.util.Collection<Object>) org.springframework.test.util.ReflectionTestUtils.invokeMethod(appointment, "domainEvents");
        boolean hasCancelEvent = events.stream().anyMatch(e -> e instanceof AppointmentCancelledEvent);
        assertTrue(hasCancelEvent);
    }

    @Test
    void testCancel_FailsIfAppointmentInThePast() {
        // Build one directly in the past bypassing factory or utilizing reflection / data builder
        Appointment pastAppointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .appointmentDate(LocalDate.now().minusDays(1))
                .appointmentTime(LocalTime.of(8,0))
                .status(AppointmentStatus.SCHEDULED)
                .build();
                
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> pastAppointment.cancel("Too late"));
        assertEquals("Cannot cancel an appointment in the past", ex.getMessage());
    }
}
