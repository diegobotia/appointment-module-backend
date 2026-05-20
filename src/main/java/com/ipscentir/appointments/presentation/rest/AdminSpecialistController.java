package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.specialist.SpecialistDTO;
import com.ipscentir.appointments.application.service.SpecialistAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminSpecialistController - READ-ONLY
 *
 * NOTA: Creación/modificación de médicos ocurre en el sistema HC (Health Center).
 * Este controlador solo proporciona acceso de lectura a médicos desde hc.medicos
 * mediante SpecialistAdminService.
 *
 * Después de Iteración 3: HC es source of truth para datos de médicos.
 */
@RestController
@RequestMapping("/api/v1/admin/specialists")
@PreAuthorize("hasRole('ADMINISTRACION')")
@RequiredArgsConstructor
@Tag(name = "Admin Specialists API", description = "Read-only specialist information from HC system")
public class AdminSpecialistController {

    private final SpecialistAdminService specialistAdminService;

    @GetMapping("/{specialistId}")
    @Operation(summary = "Get specialist by ID")
    public ResponseEntity<SpecialistDTO> getById(@PathVariable String specialistId) {
        return ResponseEntity.ok(specialistAdminService.getById(specialistId));
    }

    @GetMapping
    @Operation(summary = "List all specialists from HC")
    public ResponseEntity<List<SpecialistDTO>> listAll() {
        return ResponseEntity.ok(specialistAdminService.listAll());
    }

    @GetMapping("/active")
    @Operation(summary = "List active specialists from HC")
    public ResponseEntity<List<SpecialistDTO>> listActive() {
        return ResponseEntity.ok(specialistAdminService.listActive());
    }

    @GetMapping("/medico-number/{numeroMedico}")
    @Operation(summary = "Get specialist by HC medico number")
    public ResponseEntity<SpecialistDTO> getByMedicoNumber(@PathVariable String numeroMedico) {
        return ResponseEntity.ok(specialistAdminService.getByMedicoNumber(numeroMedico));
    }
}
