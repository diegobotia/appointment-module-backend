package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import com.ipscentir.appointments.domain.service.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationProvider notificationProvider;
    private final NotificationRepository notificationRepository;

    @Value("${notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${notifications.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Transactional
    public Notification dispatch(Notification notification) {
        if (!notificationsEnabled) {
            log.info("Notifications globally disabled; marking as sent without transmission (id={})", notification.getId());
            notification.markAsSent();
            return notificationRepository.save(notification);
        }

        boolean success = notificationProvider.sendNotification(notification);
        if (success) {
            notification.markAsSent();
        } else {
            notification.recordFailedAttempt("Provider rejected or failed transmission");
        }
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification retry(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.canRetry(maxRetryAttempts)) {
            throw new IllegalStateException("Notification cannot be retried (max attempts reached or already sent)");
        }

        notification.prepareForRetry();
        notificationRepository.save(notification);
        return dispatch(notification);
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
}
