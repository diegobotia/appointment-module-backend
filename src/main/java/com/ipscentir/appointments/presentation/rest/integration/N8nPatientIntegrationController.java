package com.ipscentir.appointments.presentation.rest.integration;

import com.ipscentir.appointments.application.dto.form.CreatePatientRegistrationRequest;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentsResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientIdentifyRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientIdentifyResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientRegistrationStatusResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nRescheduleAppointmentRequest;
import com.ipscentir.appointments.application.service.N8nPatientIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/n8n/patient")
@RequiredArgsConstructor
@Validated
@Tag(name = "n8n Patient API", description = "Conversational booking endpoints for n8n workflows")
public class N8nPatientIntegrationController {

    private final N8nPatientIntegrationService n8nPatientIntegrationService;

    @GetMapping("/registration-status")
    @Operation(summary = "Verificar si el paciente ya se registró y obtener URL del formulario")
    public ResponseEntity<N8nPatientRegistrationStatusResponse> registrationStatus(
            @RequestParam @NotBlank
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "Descripción del tipo de documento (ej. Cédula de ciudadanía), código DIAN o alias CC"
            )
            String codTipoIdentificacion,
            @RequestParam @NotBlank String numIdentificacion
    ) {
        return ResponseEntity.ok(n8nPatientIntegrationService.getPatientRegistrationStatus(
                codTipoIdentificacion,
                numIdentificacion
        ));
    }

    @PostMapping("/identify")
    @Operation(summary = "Identificar paciente por tipo y número de documento")
    public ResponseEntity<N8nPatientIdentifyResponse> identifyPatient(
            @Valid @RequestBody N8nPatientIdentifyRequest request
    ) {
        return ResponseEntity.ok(n8nPatientIntegrationService.identifyPatient(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar paciente desde flujo n8n (solo core.pacientes)")
    public ResponseEntity<PatientRegistrationResponse> registerPatient(
            @Valid @RequestBody CreatePatientRegistrationRequest request
    ) {
        PatientRegistrationResponse response = n8nPatientIntegrationService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/appointments")
    @Operation(summary = "Listar citas de un paciente por documento")
    public ResponseEntity<N8nPatientAppointmentsResponse> listAppointmentsByDocument(
            @RequestParam @NotBlank
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "Descripción del tipo de documento (ej. Cédula de ciudadanía), código DIAN o alias CC"
            )
            String codTipoIdentificacion,
            @RequestParam @NotBlank String numIdentificacion
    ) {
        return ResponseEntity.ok(n8nPatientIntegrationService.listAppointmentsByDocument(
                codTipoIdentificacion,
                numIdentificacion
        ));
    }

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

    @PatchMapping("/appointments/{appointmentId}/reschedule")
    @Operation(summary = "Reprogramar cita desde flujo n8n")
    public ResponseEntity<N8nPatientAppointmentResponse> rescheduleAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody N8nRescheduleAppointmentRequest request
    ) {
        return ResponseEntity.ok(n8nPatientIntegrationService.rescheduleAppointment(appointmentId, request));
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
