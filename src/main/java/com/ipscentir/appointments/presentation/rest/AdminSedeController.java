package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.admin.FacilityOperatingHourDTO;
import com.ipscentir.appointments.application.dto.admin.FacilityResourceInventoryDTO;
import com.ipscentir.appointments.application.dto.admin.SedeSummaryDTO;
import com.ipscentir.appointments.application.service.AdminSedeService;
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

@RestController
@RequestMapping("/api/v1/admin/sedes")
@PreAuthorize("hasRole('ADMINISTRACION')")
@RequiredArgsConstructor
@Tag(name = "Admin Sedes", description = "Sedes maestras (core.sede) y configuración operativa del módulo de citas")
public class AdminSedeController {

    private final AdminSedeService adminSedeService;

    @GetMapping
    @Operation(summary = "Listar sedes")
    public ResponseEntity<List<SedeSummaryDTO>> listSedes() {
        return ResponseEntity.ok(adminSedeService.listSedes());
    }

    @GetMapping("/{sedeId}")
    @Operation(summary = "Detalle de una sede")
    public ResponseEntity<SedeSummaryDTO> getSede(@PathVariable Integer sedeId) {
        return ResponseEntity.ok(adminSedeService.getSede(sedeId));
    }

    @GetMapping("/{sedeId}/operating-hours")
    @Operation(summary = "Horario institucional de atención de la sede")
    public ResponseEntity<List<FacilityOperatingHourDTO>> getOperatingHours(@PathVariable Integer sedeId) {
        return ResponseEntity.ok(adminSedeService.getOperatingHours(sedeId));
    }

    @GetMapping("/{sedeId}/resources")
    @Operation(summary = "Inventario de espacios físicos de la sede")
    public ResponseEntity<FacilityResourceInventoryDTO> getResources(@PathVariable Integer sedeId) {
        return ResponseEntity.ok(adminSedeService.getResourceInventory(sedeId));
    }
}
