package com.ipscentir.appointments.infrastructure.integration.twilio;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import com.ipscentir.appointments.domain.service.NotificationProvider;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TwilioNotificationProvider implements NotificationProvider {

    private final String accountSid;
    private final String authToken;
    private final String fromPhoneNumber;
    private final boolean twilioEnabled;

    public TwilioNotificationProvider(
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.phone-number:}") String fromPhoneNumber,
            @Value("${twilio.enabled:false}") boolean twilioEnabled
    ) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromPhoneNumber = fromPhoneNumber;
        this.twilioEnabled = twilioEnabled;
        
        if (twilioEnabled && accountSid != null && !accountSid.isEmpty()) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("Twilio API fully initialized inside Provider");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean sendNotification(Notification notification) {
        if (!twilioEnabled) {
            log.info("[MOCK SMS] Sending {} to {}: {}", 
                    notification.getNotificationType(), 
                    notification.getRecipient(), 
                    notification.getMessageContent());
            return true;
        }

        try {
            if (notification.getNotificationType() == NotificationType.SMS) {
                Message message = Message.creator(
                        new PhoneNumber(notification.getRecipient()),
                        new PhoneNumber(fromPhoneNumber),
                        notification.getMessageContent()
                ).create();
                log.info("SMS sent successfully with Twilio ID: {}", message.getSid());
                return true;
            } else if (notification.getNotificationType() == NotificationType.WHATSAPP) {
                Message message = Message.creator(
                        new PhoneNumber("whatsapp:" + notification.getRecipient()),
                        new PhoneNumber("whatsapp:" + fromPhoneNumber),
                        notification.getMessageContent()
                ).create();
                log.info("WhatsApp sent successfully with Twilio ID: {}", message.getSid());
                return true;
            }
            log.warn("Unsupported NotificationType passed to Twilio Provider: {}", notification.getNotificationType());
            return false;
        } catch (Exception ex) {
            log.error("Twilio Adapter caught exception sending notification: {}", ex.getMessage());
            return false;
        }
    }
}
