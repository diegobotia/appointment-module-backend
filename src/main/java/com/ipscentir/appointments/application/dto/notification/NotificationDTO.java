package com.ipscentir.appointments.application.dto.notification;

import com.ipscentir.appointments.domain.model.notification.NotificationEntityType;
import com.ipscentir.appointments.domain.model.notification.NotificationPurpose;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDTO(
        UUID id,
        NotificationEntityType entityType,
        UUID entityId,
        NotificationType notificationType,
        NotificationPurpose purpose,
        String recipient,
        String messageContent,
        NotificationStatus status,
        int retryCount,
        int maxRetryAttempts,
        boolean retryable,
        LocalDateTime createdAt,
        LocalDateTime lastAttemptAt,
        LocalDateTime sentAt,
        String failureReason
) {
}
