package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.staff.PatientSummaryDTO;
import com.ipscentir.appointments.application.service.StaffPatientLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/patients")
@PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR')")
@RequiredArgsConstructor
@Tag(name = "Staff Patients", description = "Búsqueda de pacientes para mostrador y call center")
public class StaffPatientController {

    private final StaffPatientLookupService staffPatientLookupService;

    @GetMapping("/search")
    @Operation(summary = "Buscar paciente por tipo y número de documento")
    public ResponseEntity<PatientSummaryDTO> search(
            @RequestParam String numIdentificacion,
            @RequestParam(required = false) String codTipoIdentificacion
    ) {
        return ResponseEntity.ok(staffPatientLookupService.searchByDocument(codTipoIdentificacion, numIdentificacion));
    }

    @GetMapping("/{patientId}")
    @Operation(summary = "Detalle mínimo de paciente para confirmación en UI")
    public ResponseEntity<PatientSummaryDTO> getById(@PathVariable UUID patientId) {
        return ResponseEntity.ok(staffPatientLookupService.getById(patientId));
    }
}
