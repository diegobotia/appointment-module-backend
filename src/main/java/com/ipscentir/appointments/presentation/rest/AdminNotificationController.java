package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.notification.NotificationDTO;
import com.ipscentir.appointments.application.dto.notification.NotificationPageResponse;
import com.ipscentir.appointments.application.dto.notification.NotificationSearchCriteria;
import com.ipscentir.appointments.application.service.NotificationAdminService;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@PreAuthorize("hasRole('ADMINISTRACION')")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications API", description = "Bandeja y reintentos de notificaciones")
public class AdminNotificationController {

    private final NotificationAdminService notificationAdminService;

    @GetMapping
    @Operation(summary = "List notifications with filters and pagination")
    public ResponseEntity<NotificationPageResponse> search(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationAdminService.search(
                new NotificationSearchCriteria(status, type, entityId, page, size)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by id")
    public ResponseEntity<NotificationDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationAdminService.getById(id));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed notification manually")
    public ResponseEntity<NotificationDTO> retry(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationAdminService.retry(id));
    }
}
