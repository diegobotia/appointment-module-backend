package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.specialist.AssignSpecialtyRequest;
import com.ipscentir.appointments.application.dto.specialist.CreateSpecialistRequest;
import com.ipscentir.appointments.application.dto.specialist.SpecialistDTO;
import com.ipscentir.appointments.application.dto.specialist.UpdateSpecialistRequest;
import com.ipscentir.appointments.application.service.SpecialistAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/specialists")
@RequiredArgsConstructor
@Tag(name = "Admin Specialists API", description = "Administrative specialist registration endpoints")
public class AdminSpecialistController {

    private final SpecialistAdminService specialistAdminService;

    @PostMapping
    @Operation(summary = "Create specialist")
    public ResponseEntity<SpecialistDTO> create(@Valid @RequestBody CreateSpecialistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specialistAdminService.create(request));
    }

    @PutMapping("/{specialistId}")
    @Operation(summary = "Update specialist")
    public ResponseEntity<SpecialistDTO> update(
            @PathVariable UUID specialistId,
            @Valid @RequestBody UpdateSpecialistRequest request
    ) {
        return ResponseEntity.ok(specialistAdminService.update(specialistId, request));
    }

    @PatchMapping("/{specialistId}/deactivate")
    @Operation(summary = "Deactivate specialist")
    public ResponseEntity<SpecialistDTO> deactivate(@PathVariable UUID specialistId) {
        return ResponseEntity.ok(specialistAdminService.deactivate(specialistId));
    }

    @PostMapping("/{specialistId}/specialties")
    @Operation(summary = "Assign specialty to specialist")
    public ResponseEntity<SpecialistDTO> assignSpecialty(
            @PathVariable UUID specialistId,
            @Valid @RequestBody AssignSpecialtyRequest request
    ) {
        return ResponseEntity.ok(specialistAdminService.assignSpecialty(specialistId, request));
    }

    @DeleteMapping("/{specialistId}/specialties/{specialtyId}")
    @Operation(summary = "Remove specialty from specialist")
    public ResponseEntity<SpecialistDTO> removeSpecialty(
            @PathVariable UUID specialistId,
            @PathVariable UUID specialtyId
    ) {
        return ResponseEntity.ok(specialistAdminService.removeSpecialty(specialistId, specialtyId));
    }
}
