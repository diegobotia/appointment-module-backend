package com.ipscentir.appointments.presentation.rest.integration;

import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventResponse;
import com.ipscentir.appointments.application.service.N8nPatientIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/n8n/webhooks")
@RequiredArgsConstructor
@Tag(name = "n8n Webhooks API", description = "Webhook endpoint for event acknowledgements and automation receipts")
public class N8nWebhookController {

    private final N8nPatientIntegrationService n8nPatientIntegrationService;

    @PostMapping("/events")
    @Operation(summary = "Receive or acknowledge automation events")
    public ResponseEntity<N8nWebhookEventResponse> handleEvent(@Valid @RequestBody N8nWebhookEventRequest request) {
        return ResponseEntity.ok(n8nPatientIntegrationService.handleWebhookEvent(request));
    }
}