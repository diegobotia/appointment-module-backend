package com.ipscentir.appointments.infrastructure.integration.resend;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.service.NotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ResendNotificationProvider implements NotificationProvider {

    private final String apiKey;
    private final String fromEmail;
    private final boolean enabled;
    private final RestTemplate restTemplate;

    public ResendNotificationProvider(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from-email:onboarding@resend.dev}") String fromEmail,
            @Value("${resend.enabled:false}") boolean enabled
    ) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.enabled = enabled;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean sendNotification(Notification notification) {
        if (!enabled) {
            log.info("[MOCK EMAIL] Sending EMAIL to {}: {}", 
                    notification.getRecipient(), 
                    notification.getMessageContent());
            return true;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", fromEmail);
            body.put("to", notification.getRecipient());
            body.put("subject", "Recordatorio de Cita Médica - IPS Centir");
            body.put("html", "<p>" + notification.getMessageContent() + "</p>");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity("https://api.resend.com/emails", entity, String.class);
            log.info("Email sent successfully to {} via Resend", notification.getRecipient());
            return true;
        } catch (Exception e) {
            log.error("Failed to send email via Resend: {}", e.getMessage());
            return false;
        }
    }
}
