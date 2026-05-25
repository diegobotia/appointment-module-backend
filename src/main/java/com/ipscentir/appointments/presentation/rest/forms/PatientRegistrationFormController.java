package com.ipscentir.appointments.presentation.rest.forms;

import com.ipscentir.appointments.application.dto.form.CreatePatientRegistrationRequest;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationFormConfigResponse;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationResponse;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationStatusResponse;
import com.ipscentir.appointments.application.service.PatientRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/forms/patients")
@RequiredArgsConstructor
@Validated
@Tag(name = "Patient Registration Form API", description = "Formulario público de registro de pacientes (sin autenticación JWT)")
public class PatientRegistrationFormController {

    private final PatientRegistrationService patientRegistrationService;

    @GetMapping({"", "/config"})
    @Operation(summary = "Obtener configuración del formulario de registro")
    public ResponseEntity<PatientRegistrationFormConfigResponse> getFormConfig() {
        return ResponseEntity.ok(patientRegistrationService.getFormConfig());
    }

    @GetMapping("/status")
    @Operation(summary = "Consultar si un paciente ya fue registrado por documento")
    public ResponseEntity<PatientRegistrationStatusResponse> getRegistrationStatus(
            @RequestParam @NotBlank String codTipoIdentificacion,
            @RequestParam @NotBlank String numIdentificacion
    ) {
        return ResponseEntity.ok(patientRegistrationService.getRegistrationStatus(
                codTipoIdentificacion,
                numIdentificacion
        ));
    }

    @PostMapping
    @Operation(summary = "Registrar paciente desde formulario web")
    public ResponseEntity<PatientRegistrationResponse> registerPatient(
            @Valid @RequestBody CreatePatientRegistrationRequest request
    ) {
        PatientRegistrationResponse response = patientRegistrationService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
