package com.ipscentir.appointments.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.admin.BookingChannelSummaryDTO;
import com.ipscentir.appointments.application.dto.admin.N8nJournalEntryDTO;
import com.ipscentir.appointments.application.dto.admin.N8nJournalPageResponse;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.DomainEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminN8nJournalService {

    private final DomainEventRepository domainEventRepository;
    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public N8nJournalPageResponse search(String eventType, Boolean published, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }

        long total = domainEventRepository.countSearch(eventType, published);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        List<N8nJournalEntryDTO> content = domainEventRepository
                .search(eventType, published, page * size, size)
                .stream()
                .map(this::toDto)
                .toList();

        long n8n = appointmentRepository.countByBookingChannel(BookingChannel.N8N);
        long staff = appointmentRepository.countByBookingChannel(BookingChannel.STAFF);

        return new N8nJournalPageResponse(content, page, size, total, totalPages, n8n, staff);
    }

    @Transactional(readOnly = true)
    public BookingChannelSummaryDTO bookingChannelSummary() {
        long n8n = appointmentRepository.countByBookingChannel(BookingChannel.N8N);
        long staff = appointmentRepository.countByBookingChannel(BookingChannel.STAFF);
        return new BookingChannelSummaryDTO(n8n, staff, n8n + staff);
    }

    private N8nJournalEntryDTO toDto(DomainEventRecord record) {
        BookingChannel channel = BookingChannel.STAFF;
        String conversationId = null;

        try {
            JsonNode root = objectMapper.readTree(record.getEventData());
            if (root.hasNonNull("bookingChannel")) {
                channel = BookingChannel.valueOf(root.get("bookingChannel").asText());
            }
            if (root.hasNonNull("n8nConversationId")) {
                conversationId = root.get("n8nConversationId").asText();
            }
        } catch (Exception ignored) {
            // eventData legacy sin canal
        }

        return new N8nJournalEntryDTO(
                record.getId(),
                record.getEventType(),
                record.getAggregateId(),
                channel,
                conversationId,
                record.isPublished(),
                record.getOccurredOn(),
                record.getPublishedAt(),
                record.getEventData()
        );
    }
}
