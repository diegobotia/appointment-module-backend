package com.ipscentir.appointments.application.dto.admin;

import com.ipscentir.appointments.domain.model.appointment.BookingChannel;

import java.time.LocalDateTime;
import java.util.UUID;

public record N8nJournalEntryDTO(
        UUID eventId,
        String eventType,
        UUID appointmentId,
        BookingChannel bookingChannel,
        String n8nConversationId,
        boolean published,
        LocalDateTime occurredOn,
        LocalDateTime publishedAt,
        String eventData
) {
}
