package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.specialist.SpecialistDTO;
import com.ipscentir.appointments.application.dto.specialty.SpecialtyDTO;
import com.ipscentir.appointments.application.service.SpecialtyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
@Tag(name = "Specialties API", description = "Specialty catalog and specialist lookup endpoints")
public class SpecialtyController {

    private final SpecialtyQueryService specialtyQueryService;

    @GetMapping
    @Operation(summary = "List active specialties")
    public ResponseEntity<List<SpecialtyDTO>> listSpecialties() {
        return ResponseEntity.ok(specialtyQueryService.listSpecialties());
    }

    @GetMapping("/{specialtyId}/specialists")
    @Operation(summary = "List specialists by specialty")
    public ResponseEntity<Page<SpecialistDTO>> listSpecialistsBySpecialty(
            @PathVariable UUID specialtyId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @Parameter(hidden = true) Pageable pageable
    ) {
        return ResponseEntity.ok(specialtyQueryService.listSpecialistsBySpecialty(specialtyId, activeOnly, pageable));
    }
}