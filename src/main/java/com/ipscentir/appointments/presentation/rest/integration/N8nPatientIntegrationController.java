package com.ipscentir.appointments.presentation.rest.integration;

import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityResponse;
import com.ipscentir.appointments.application.service.N8nPatientIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/n8n/patient")
@RequiredArgsConstructor
@Tag(name = "n8n Patient API", description = "Conversational booking endpoints for n8n workflows")
public class N8nPatientIntegrationController {

    private final N8nPatientIntegrationService n8nPatientIntegrationService;

    @PostMapping("/availability")
    @Operation(summary = "Get available appointment slots for chat workflows")
    public ResponseEntity<N8nPatientAvailabilityResponse> availability(@Valid @RequestBody N8nPatientAvailabilityRequest request) {
        return ResponseEntity.ok(n8nPatientIntegrationService.getAvailability(request));
    }

    @PostMapping("/appointments")
    @Operation(summary = "Create appointment from n8n chat flow")
    public ResponseEntity<N8nPatientAppointmentResponse> createAppointment(@Valid @RequestBody N8nPatientAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(n8nPatientIntegrationService.createAppointment(request));
    }

    @PostMapping("/appointments/{appointmentId}/cancel")
    @Operation(summary = "Cancel appointment from n8n chat flow")
    public ResponseEntity<N8nCancelAppointmentResponse> cancelAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody N8nCancelAppointmentRequest request
    ) {
        return ResponseEntity.ok(n8nPatientIntegrationService.cancelAppointment(appointmentId, request));
    }
}