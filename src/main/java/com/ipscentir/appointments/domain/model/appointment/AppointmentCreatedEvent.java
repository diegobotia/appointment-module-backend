package com.ipscentir.appointments.domain.model.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentCreatedEvent(
        UUID appointmentId,
        UUID patientId,
        String doctorId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        AppointmentType appointmentType,
        BookingChannel bookingChannel,
        String n8nConversationId
) {}
