package com.ipscentir.appointments.domain.model.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentCancelledEvent(
        UUID appointmentId,
        UUID patientId,
        String doctorId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String cancellationReason
) {}
