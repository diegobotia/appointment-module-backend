package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.CatalogItemDTO;
import com.ipscentir.appointments.application.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogs")
@RequiredArgsConstructor
@Tag(name = "Catalogs API", description = "Versioned catalogs for appointment orchestration")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/appointment-service-types")
    @Operation(summary = "List appointment service types", description = "Returns the canonical list of appointment service types")
    public ResponseEntity<List<CatalogItemDTO>> listAppointmentServiceTypes() {
        return ResponseEntity.ok(catalogService.listAppointmentServiceTypes());
    }

    @GetMapping("/specialties")
    @Operation(summary = "List specialties", description = "Returns the canonical list of medical specialties")
    public ResponseEntity<List<CatalogItemDTO>> listSpecialties() {
        return ResponseEntity.ok(catalogService.listSpecialties());
    }
}
