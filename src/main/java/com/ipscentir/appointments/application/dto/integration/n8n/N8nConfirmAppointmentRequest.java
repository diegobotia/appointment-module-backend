package com.ipscentir.appointments.application.dto.integration.n8n;

public record N8nConfirmAppointmentRequest(
        String reason,
        String confirmedAt
) {}
