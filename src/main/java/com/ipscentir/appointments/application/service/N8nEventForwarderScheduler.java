package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import com.ipscentir.appointments.domain.repository.DomainEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class N8nEventForwarderScheduler {

    private final DomainEventRepository domainEventRepository;
    private final RestTemplate restTemplate;
    private final String n8nWebhookUrl;
    private final String n8nApiKey;

    public N8nEventForwarderScheduler(
            DomainEventRepository domainEventRepository,
            @Value("${integration.n8n.webhook-url:}") String n8nWebhookUrl,
            @Value("${security.n8n.api-key}") String n8nApiKey
    ) {
        this.domainEventRepository = domainEventRepository;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);
        this.n8nWebhookUrl = n8nWebhookUrl;
        this.n8nApiKey = n8nApiKey;
    }

    @Scheduled(cron = "${appointments.n8n.event-forwarder-cron:0 */2 * * * *}")
    public void forwardPendingEvents() {
        if (n8nWebhookUrl == null || n8nWebhookUrl.isBlank()) {
            log.debug("n8n webhook URL not configured; skipping event forwarding");
            return;
        }

        List<DomainEventRecord> pendingEvents = domainEventRepository
                .findByPublishedFalseOrderByOccurredOnAsc();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Forwarding {} pending domain events to n8n", pendingEvents.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(n8nApiKey);

        for (DomainEventRecord event : pendingEvents) {
            try {
                HttpEntity<String> request = new HttpEntity<>(event.getEventData(), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(
                        n8nWebhookUrl, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    markPublished(event);
                    log.debug("Forwarded event {} ({}) to n8n", event.getId(), event.getEventType());
                } else {
                    log.warn("n8n returned {} for event {}; will retry",
                            response.getStatusCode(), event.getId());
                }
            } catch (Exception e) {
                log.error("Failed to forward event {} to n8n: {}", event.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void markPublished(DomainEventRecord event) {
        event.markPublished();
        domainEventRepository.save(event);
    }
}
