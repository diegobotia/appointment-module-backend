package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.medico.MedicoPageResponse;
import com.ipscentir.appointments.application.dto.medico.MedicoSearchCriteria;
import com.ipscentir.appointments.application.dto.medico.MedicoSummaryDTO;
import com.ipscentir.appointments.application.service.AdminMedicoService;
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

@RestController
@RequestMapping({"/api/v1/admin/medicos", "/api/v1/admin/specialists"})
@PreAuthorize("hasRole('ADMINISTRACION')")
@RequiredArgsConstructor
@Tag(name = "Admin Medicos", description = "Directorio hc.medicos para configuración administrativa")
public class AdminMedicoController {

    private final AdminMedicoService adminMedicoService;

    @GetMapping
    @Operation(summary = "Buscar médicos por nombre, documento, registro o especialidad")
    public ResponseEntity<MedicoPageResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String numDoc,
            @RequestParam(required = false) String registro,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminMedicoService.search(
                new MedicoSearchCriteria(q, numDoc, registro, specialty, active, page, size)
        ));
    }

    @GetMapping("/{medicoId}")
    @Operation(summary = "Detalle de un médico")
    public ResponseEntity<MedicoSummaryDTO> getById(@PathVariable String medicoId) {
        return ResponseEntity.ok(adminMedicoService.getById(medicoId));
    }
}
