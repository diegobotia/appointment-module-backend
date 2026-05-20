package com.ipscentir.appointments.application.dto.integration.n8n;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record N8nPatientAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull String doctorId,
        @NotNull N8nFacilityId facilityId,
        String secondaryDoctorId,
        @NotNull UUID scheduleId,
        @NotNull @FutureOrPresent LocalDate appointmentDate,
        @NotNull LocalTime appointmentTime,
        String reason,
        String conversationId,
        String requestId
) {
    public String resolveIdempotencyKey() {
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationId.trim();
        }
        return null;
    }
}