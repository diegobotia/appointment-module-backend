package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.notification.NotificationDTO;
import com.ipscentir.appointments.application.dto.notification.NotificationPageResponse;
import com.ipscentir.appointments.application.dto.notification.NotificationSearchCriteria;
import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationAdminService {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional(readOnly = true)
    public NotificationPageResponse search(NotificationSearchCriteria criteria) {
        long total = notificationRepository.countSearch(
                criteria.status(),
                criteria.type(),
                criteria.entityId()
        );
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / criteria.size());

        List<NotificationDTO> content = notificationRepository.search(
                criteria.status(),
                criteria.type(),
                criteria.entityId(),
                criteria.page() * criteria.size(),
                criteria.size()
        ).stream().map(this::toDto).toList();

        return new NotificationPageResponse(
                content,
                criteria.page(),
                criteria.size(),
                total,
                totalPages
        );
    }

    @Transactional(readOnly = true)
    public NotificationDTO getById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        return toDto(notification);
    }

    @Transactional
    public NotificationDTO retry(UUID id) {
        Notification updated = notificationDispatchService.retry(id);
        return toDto(updated);
    }

    private NotificationDTO toDto(Notification notification) {
        int maxAttempts = notificationDispatchService.getMaxRetryAttempts();
        return new NotificationDTO(
                notification.getId(),
                notification.getEntityType(),
                notification.getEntityId(),
                notification.getNotificationType(),
                notification.getPurpose(),
                notification.getRecipient(),
                notification.getMessageContent(),
                notification.getStatus(),
                notification.getRetryCount(),
                maxAttempts,
                notification.canRetry(maxAttempts),
                notification.getCreatedAt(),
                notification.getLastAttemptAt(),
                notification.getSentAt(),
                notification.getFailureReason()
        );
    }
}
