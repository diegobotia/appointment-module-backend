package com.ipscentir.appointments.application.dto.notification;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
