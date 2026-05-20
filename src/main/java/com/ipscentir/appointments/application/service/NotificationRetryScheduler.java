package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private static final int BATCH_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService notificationDispatchService;

    @Value("${notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${notifications.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Scheduled(cron = "${notifications.retry-cron:0 */15 * * * *}")
    public void processFailedNotifications() {
        if (!notificationsEnabled) {
            return;
        }

        List<Notification> retryable = notificationRepository.findRetryable(maxRetryAttempts, BATCH_SIZE);
        if (retryable.isEmpty()) {
            return;
        }

        log.info("Processing {} failed notifications for retry (maxAttempts={})", retryable.size(), maxRetryAttempts);
        for (Notification notification : retryable) {
            try {
                notificationDispatchService.retry(notification.getId());
            } catch (Exception ex) {
                log.error("Retry failed for notification {}: {}", notification.getId(), ex.getMessage());
            }
        }
    }
}
