package com.ipscentir.appointments.domain.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "notifications", schema = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    @Builder.Default
    private NotificationEntityType entityType = NotificationEntityType.APPOINTMENT;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private NotificationPurpose purpose;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "message_content")
    private String messageContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failure_reason")
    private String failureReason;

    public static Notification create(
            NotificationEntityType entityType,
            UUID entityId,
            NotificationType type,
            NotificationPurpose purpose,
            String recipient,
            String messageContent
    ) {
        return Notification.builder()
                .entityType(entityType)
                .entityId(entityId)
                .notificationType(type)
                .purpose(purpose)
                .recipient(recipient)
                .messageContent(messageContent)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public static Notification create(
            UUID entityId,
            NotificationType type,
            NotificationPurpose purpose,
            String recipient,
            String messageContent
    ) {
        return create(NotificationEntityType.APPOINTMENT, entityId, type, purpose, recipient, messageContent);
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastAttemptAt = LocalDateTime.now();
        this.failureReason = null;
    }

    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
        this.lastAttemptAt = LocalDateTime.now();
    }

    public void recordFailedAttempt(String reason) {
        this.retryCount++;
        markAsFailed(reason);
    }

    public boolean canRetry(int maxAttempts) {
        return status != NotificationStatus.SENT && retryCount < maxAttempts;
    }

    public void prepareForRetry() {
        this.status = NotificationStatus.PENDING;
    }
}
