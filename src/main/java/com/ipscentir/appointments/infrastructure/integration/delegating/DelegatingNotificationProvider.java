package com.ipscentir.appointments.infrastructure.integration.delegating;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import com.ipscentir.appointments.domain.service.NotificationProvider;
import com.ipscentir.appointments.infrastructure.integration.resend.ResendNotificationProvider;
import com.ipscentir.appointments.infrastructure.integration.twilio.TwilioNotificationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class DelegatingNotificationProvider implements NotificationProvider {

    private final TwilioNotificationProvider twilioProvider;
    private final ResendNotificationProvider resendProvider;

    @Override
    public boolean sendNotification(Notification notification) {
        if (notification.getNotificationType() == NotificationType.EMAIL) {
            return resendProvider.sendNotification(notification);
        }
        return twilioProvider.sendNotification(notification);
    }
}
