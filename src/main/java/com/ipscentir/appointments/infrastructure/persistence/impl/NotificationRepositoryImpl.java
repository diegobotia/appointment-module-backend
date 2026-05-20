package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Notification> findByEntityId(UUID entityId) {
        return jpaRepository.findByEntityId(entityId);
    }

    @Override
    public List<Notification> findRetryable(int maxAttempts, int limit) {
        return jpaRepository.findRetryable(maxAttempts, PageRequest.of(0, limit));
    }

    @Override
    public List<Notification> search(NotificationStatus status, NotificationType type, UUID entityId, int offset, int limit) {
        int page = limit > 0 ? offset / limit : 0;
        return jpaRepository.search(status, type, entityId, PageRequest.of(page, Math.max(limit, 1)));
    }

    @Override
    public long countSearch(NotificationStatus status, NotificationType type, UUID entityId) {
        return jpaRepository.countSearch(status, type, entityId);
    }

    @Override
    public long countByStatus(NotificationStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countRetryable(int maxAttempts) {
        return jpaRepository.countRetryable(maxAttempts);
    }
}
