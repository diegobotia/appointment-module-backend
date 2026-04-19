package com.ipscentir.appointments.application.dto.integration.n8n;

import java.time.LocalDateTime;
import java.util.UUID;

public record N8nWebhookEventResponse(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        boolean published,
        String status,
        String message,
        LocalDateTime timestamp
) {}