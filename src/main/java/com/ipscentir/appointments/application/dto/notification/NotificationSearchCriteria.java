package com.ipscentir.appointments.application.dto.notification;

import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;

import java.util.UUID;

public record NotificationSearchCriteria(
        NotificationStatus status,
        NotificationType type,
        UUID entityId,
        int page,
        int size
) {
    public NotificationSearchCriteria {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }
}
