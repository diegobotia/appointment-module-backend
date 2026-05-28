package com.ipscentir.appointments.domain.model.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;



@Entity
@Table(name = "domain_events", schema = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DomainEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private String eventData;

    @Column(name = "occurred_on", updatable = false, insertable = false)
    private LocalDateTime occurredOn;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static DomainEventRecord unpublished(String eventType, UUID aggregateId, String eventData) {
        return DomainEventRecord.builder()
                .eventType(eventType)
                .aggregateId(aggregateId)
                .eventData(eventData)
                .published(false)
                .build();
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }
}