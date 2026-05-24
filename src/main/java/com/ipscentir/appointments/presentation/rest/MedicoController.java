package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.availability.MedicoAvailabilityResponse;
import com.ipscentir.appointments.application.dto.medico.MedicoAvailableDTO;
import com.ipscentir.appointments.application.service.MedicoApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/medicos", "/api/v1/doctors"})
@RequiredArgsConstructor
@Tag(name = "Medicos", description = "Consulta de médicos y disponibilidad (hc.medicos)")
public class MedicoController {

    private final MedicoApplicationService medicoApplicationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO', 'FACTURACION')")
    @Operation(summary = "Listar médicos disponibles")
    public ResponseEntity<List<MedicoAvailableDTO>> listAvailableMedicos(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) Integer sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availabilityDate
    ) {
        return ResponseEntity.ok(medicoApplicationService.findAvailableMedicos(specialty, sedeId, availabilityDate));
    }

    @GetMapping("/{medicoId}/availability")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO', 'FACTURACION')")
    @Operation(summary = "Obtener disponibilidad de un médico en rango de fechas")
    public ResponseEntity<MedicoAvailabilityResponse> getMedicoAvailability(
            @PathVariable String medicoId,
            @RequestParam @NotNull Integer sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        LocalDate fromDate = from != null ? from : startDate;
        return ResponseEntity.ok(medicoApplicationService.getMedicoAvailability(medicoId, sedeId, fromDate, to));
    }
}
