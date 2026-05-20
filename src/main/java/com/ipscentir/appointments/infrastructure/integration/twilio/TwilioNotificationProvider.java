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

    private final String fromPhoneNumber;
    private final String whatsappFromNumber;
    private final boolean twilioEnabled;

    public TwilioNotificationProvider(
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.phone-number:}") String fromPhoneNumber,
            @Value("${twilio.whatsapp-number:}") String whatsappFromNumber,
            @Value("${twilio.enabled:false}") boolean twilioEnabled
    ) {
        this.fromPhoneNumber = fromPhoneNumber;
        this.whatsappFromNumber = whatsappFromNumber != null && !whatsappFromNumber.isBlank()
                ? whatsappFromNumber
                : fromPhoneNumber;
        this.twilioEnabled = twilioEnabled;

        if (twilioEnabled && accountSid != null && !accountSid.isEmpty()) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("Twilio API initialized");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean sendNotification(Notification notification) {
        if (!twilioEnabled) {
            log.info("[MOCK TWILIO] {} -> {}: {}",
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
                log.info("SMS sent, Twilio SID: {}", message.getSid());
                return true;
            }

            if (notification.getNotificationType() == NotificationType.WHATSAPP) {
                Message message = Message.creator(
                        new PhoneNumber("whatsapp:" + notification.getRecipient()),
                        new PhoneNumber("whatsapp:" + whatsappFromNumber),
                        notification.getMessageContent()
                ).create();
                log.info("WhatsApp sent, Twilio SID: {}", message.getSid());
                return true;
            }

            log.warn("Unsupported type for Twilio: {}", notification.getNotificationType());
            return false;
        } catch (Exception ex) {
            log.error("Twilio error: {}", ex.getMessage());
            return false;
        }
    }
}
