package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventResponse;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCancelledEvent;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCreatedEvent;
import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import com.ipscentir.appointments.domain.repository.DomainEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
                serialize(Map.of(
                        "appointmentId", event.appointmentId(),
                        "patientId", event.patientId(),
                        "doctorId", event.doctorId(),
                        "appointmentDate", event.appointmentDate(),
                        "appointmentTime", event.appointmentTime(),
                        "appointmentType", event.appointmentType().name()
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
                        "appointmentDate", event.appointmentDate(),
                        "appointmentTime", event.appointmentTime(),
                        "cancellationReason", event.cancellationReason()
                ))
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

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize webhook payload", e);
        }
    }
}