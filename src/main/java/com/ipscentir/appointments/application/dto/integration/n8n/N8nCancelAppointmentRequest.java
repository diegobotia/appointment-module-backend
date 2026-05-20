package com.ipscentir.appointments.application.dto.integration.n8n;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record N8nCancelAppointmentRequest(
        @NotBlank String reason,
        UUID patientId
) {}