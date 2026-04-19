package com.ipscentir.appointments.application.dto.integration.n8n;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record N8nWebhookEventRequest(
        UUID eventId,
        @NotBlank String eventType,
        @NotNull UUID aggregateId,
        @NotBlank String source,
        Map<String, Object> payload,
        Boolean published
) {}