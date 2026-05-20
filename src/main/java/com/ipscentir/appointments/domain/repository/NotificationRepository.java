package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findByEntityId(UUID entityId);

    List<Notification> findRetryable(int maxAttempts, int limit);

    List<Notification> search(
            NotificationStatus status,
            NotificationType type,
            UUID entityId,
            int offset,
            int limit
    );

    long countSearch(NotificationStatus status, NotificationType type, UUID entityId);

    long countByStatus(NotificationStatus status);

    long countRetryable(int maxAttempts);
}
