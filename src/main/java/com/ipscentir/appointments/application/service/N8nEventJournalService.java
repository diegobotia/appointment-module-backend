package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventResponse;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCancelledEvent;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCreatedEvent;
import com.ipscentir.appointments.domain.model.appointment.AppointmentRescheduledEvent;
import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import com.ipscentir.appointments.domain.repository.DomainEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class N8nEventJournalService {

    private final DomainEventRepository domainEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DomainEventRecord recordAppointmentCreated(AppointmentCreatedEvent event) {
        return domainEventRepository.save(DomainEventRecord.unpublished(
                "APPOINTMENT_CREATED",
                event.appointmentId(),
                serialize(eventPayload(
                        event.appointmentId(),
                        event.patientId(),
                        event.doctorId(),
                        event.appointmentDate().toString(),
                        event.appointmentTime().toString(),
                        event.appointmentType().name(),
                        event.bookingChannel().name(),
                        event.n8nConversationId()
                ))
        ));
    }

    @Transactional
    public DomainEventRecord recordAppointmentCancelled(AppointmentCancelledEvent event) {
        return domainEventRepository.save(DomainEventRecord.unpublished(
                "APPOINTMENT_CANCELLED",
                event.appointmentId(),
                serialize(Map.of(
                        "appointmentId", event.appointmentId(),
                        "patientId", event.patientId(),
                        "doctorId", event.doctorId(),
                        "appointmentDate", event.appointmentDate().toString(),
                        "appointmentTime", event.appointmentTime().toString(),
                        "cancellationReason", event.cancellationReason()
                ))
        ));
    }

    @Transactional
    public DomainEventRecord recordAppointmentRescheduled(AppointmentRescheduledEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", event.appointmentId());
        payload.put("patientId", event.patientId());
        payload.put("doctorId", event.doctorId());
        payload.put("previousDate", event.previousDate().toString());
        payload.put("previousTime", event.previousTime().toString());
        payload.put("newDate", event.newDate().toString());
        payload.put("newTime", event.newTime().toString());
        payload.put("bookingChannel", event.channel().name());
        payload.put("n8nConversationId", event.n8nConversationId());

        return domainEventRepository.save(DomainEventRecord.unpublished(
                "APPOINTMENT_RESCHEDULED",
                event.appointmentId(),
                serialize(payload)
        ));
    }

    @Transactional
    public N8nWebhookEventResponse handleWebhookEvent(N8nWebhookEventRequest request) {
        if (request.eventId() != null && Boolean.TRUE.equals(request.published())) {
            DomainEventRecord existing = domainEventRepository.findById(request.eventId())
                    .orElseThrow(() -> new IllegalArgumentException("Event not found"));
            existing.markPublished();
            domainEventRepository.save(existing);
            return new N8nWebhookEventResponse(
                    existing.getId(),
                    existing.getEventType(),
                    existing.getAggregateId(),
                    true,
                    "ACKNOWLEDGED",
                    "Event marked as published",
                    LocalDateTime.now()
            );
        }

        DomainEventRecord domainEvent = DomainEventRecord.builder()
                .eventType(request.eventType())
                .aggregateId(request.aggregateId())
                .eventData(serialize(request.payload() == null ? Map.of() : request.payload()))
                .published(Boolean.TRUE.equals(request.published()))
                .build();

        DomainEventRecord saved = domainEventRepository.save(domainEvent);
        if (saved.isPublished()) {
            saved.markPublished();
            saved = domainEventRepository.save(saved);
        }

        return new N8nWebhookEventResponse(
                saved.getId(),
                saved.getEventType(),
                saved.getAggregateId(),
                saved.isPublished(),
                "RECEIVED",
                "Event stored successfully",
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<DomainEventRecord> listPendingEvents() {
        return domainEventRepository.findByPublishedFalseOrderByOccurredOnAsc();
    }

    private Map<String, Object> eventPayload(
            Object appointmentId,
            Object patientId,
            Object doctorId,
            String appointmentDate,
            String appointmentTime,
            String appointmentType,
            String bookingChannel,
            String n8nConversationId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", appointmentId);
        payload.put("patientId", patientId);
        payload.put("doctorId", doctorId);
        payload.put("appointmentDate", appointmentDate);
        payload.put("appointmentTime", appointmentTime);
        payload.put("appointmentType", appointmentType);
        payload.put("bookingChannel", bookingChannel);
        if (n8nConversationId != null && !n8nConversationId.isBlank()) {
            payload.put("n8nConversationId", n8nConversationId);
        }
        return payload;
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize webhook payload", e);
        }
    }
}
