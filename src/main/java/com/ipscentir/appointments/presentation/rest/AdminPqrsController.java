package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.PqrsDTO;
import com.ipscentir.appointments.application.dto.admin.PqrsPageResponse;
import com.ipscentir.appointments.application.dto.admin.UpdatePqrsStatusRequest;
import com.ipscentir.appointments.application.service.PqrsAdminService;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/pqrs")
@PreAuthorize("hasRole('ADMINISTRACION')")
@RequiredArgsConstructor
@Tag(name = "Admin PQRS API", description = "Bandeja administrativa de PQRS")
public class AdminPqrsController {

    private final PqrsAdminService pqrsAdminService;

    @GetMapping
    @Operation(summary = "Listar PQRS con filtros y paginación")
    public ResponseEntity<PqrsPageResponse> search(
            @RequestParam(required = false) PqrsStatus status,
            @RequestParam(required = false) PqrsType tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(pqrsAdminService.search(status, tipo, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de PQRS")
    public ResponseEntity<PqrsDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(pqrsAdminService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Actualizar estado de PQRS")
    public ResponseEntity<PqrsDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePqrsStatusRequest request
    ) {
        return ResponseEntity.ok(pqrsAdminService.updateStatus(id, request));
    }
}
