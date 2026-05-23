package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationPurpose;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import com.ipscentir.appointments.domain.service.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationProvider notificationProvider;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.ipscentir.appointments.infrastructure.observability.AppointmentsMetrics appointmentsMetrics;

    @InjectMocks
    private NotificationDispatchService notificationDispatchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationDispatchService, "notificationsEnabled", true);
        ReflectionTestUtils.setField(notificationDispatchService, "maxRetryAttempts", 3);
    }

    @Test
    void dispatchMarksSentOnProviderSuccess() {
        when(notificationProvider.sendNotification(any())).thenReturn(true);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.SMS,
                NotificationPurpose.APPOINTMENT_CREATED,
                "+573001111111",
                "Test"
        );

        Notification result = notificationDispatchService.dispatch(notification);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        verify(notificationProvider).sendNotification(notification);
    }

    @Test
    void dispatchIncrementsRetryOnFailure() {
        when(notificationProvider.sendNotification(any())).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.EMAIL,
                NotificationPurpose.REMINDER_24H,
                "a@b.com",
                "Test"
        );

        Notification result = notificationDispatchService.dispatch(notification);
        assertEquals(NotificationStatus.FAILED, result.getStatus());
        assertEquals(1, result.getRetryCount());
    }

    @Test
    void retryThrowsWhenMaxAttemptsReached() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.SMS,
                NotificationPurpose.APPOINTMENT_CREATED,
                "+57300",
                "x"
        );
        notification.markAsFailed("max reached");
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "retryCount", 3);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        assertThrows(IllegalStateException.class, () -> notificationDispatchService.retry(id));
    }
}
