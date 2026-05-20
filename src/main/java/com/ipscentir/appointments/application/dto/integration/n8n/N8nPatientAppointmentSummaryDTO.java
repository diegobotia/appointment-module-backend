package com.ipscentir.appointments.application.dto.integration.n8n;

import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record N8nPatientAppointmentSummaryDTO(
        UUID appointmentId,
        UUID patientId,
        String doctorId,
        UUID facilityId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        AppointmentType appointmentType,
        AppointmentStatus status,
        String reason
) {
}
