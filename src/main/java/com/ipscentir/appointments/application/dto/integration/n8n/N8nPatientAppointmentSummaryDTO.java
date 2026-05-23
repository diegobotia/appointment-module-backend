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
        Integer sedeId,
        /** Código n8n de sede: BELEN, CONQUISTADORES */
        String facilityCode,
        UUID scheduleId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        AppointmentType appointmentType,
        /** Código del catálogo cerrado (p. ej. MEDICO_FISIATRIA). Siempre va junto con specialty. */
        String serviceType,
        /** Nombre del mismo ítem del catálogo (p. ej. Medico fisiatria). Null si serviceType es null. */
        String specialty,
        AppointmentStatus status,
        String reason
) {
}
