package com.ipscentir.appointments.domain.model.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentCreatedEvent(
        UUID appointmentId,
        UUID patientId,
        UUID doctorId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        AppointmentType appointmentType
) {}
