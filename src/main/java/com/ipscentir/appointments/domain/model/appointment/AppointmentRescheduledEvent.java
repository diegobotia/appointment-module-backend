package com.ipscentir.appointments.domain.model.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentRescheduledEvent(
        UUID appointmentId,
        UUID patientId,
        String doctorId,
        LocalDate previousDate,
        LocalTime previousTime,
        LocalDate newDate,
        LocalTime newTime,
        BookingChannel channel,
        String n8nConversationId
) {
}
