package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanBlockRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.PublishSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanPageResponse;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanSearchCriteria;
import com.ipscentir.appointments.application.service.SchedulePlanAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/schedule-plans")
@PreAuthorize("hasRole('ADMINISTRACION')")
@RequiredArgsConstructor
@Tag(name = "Admin Schedule Plans API", description = "Planificación por rango de fechas y publicación de agendas")
public class AdminSchedulePlanController {

    private final SchedulePlanAdminService schedulePlanAdminService;

    @PostMapping
    @Operation(summary = "Create schedule plan version")
    public ResponseEntity<SchedulePlanDTO> create(@Valid @RequestBody CreateSchedulePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulePlanAdminService.createPlan(request));
    }

    @PostMapping("/{planId}/blocks")
    @Operation(summary = "Create schedule block range")
    public ResponseEntity<SchedulePlanDTO> addBlock(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateSchedulePlanBlockRequest request
    ) {
        return ResponseEntity.ok(schedulePlanAdminService.addBlock(planId, request));
    }

    @PostMapping("/{planId}/publish")
    @Operation(summary = "Publish schedule plan version and materialize operational schedules")
    public ResponseEntity<SchedulePlanDTO> publish(
            @PathVariable UUID planId,
            @Valid @RequestBody PublishSchedulePlanRequest request
    ) {
        return ResponseEntity.ok(schedulePlanAdminService.publish(planId, request));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get schedule plan by id")
    public ResponseEntity<SchedulePlanDTO> getById(@PathVariable UUID planId) {
        return ResponseEntity.ok(schedulePlanAdminService.getById(planId));
    }

    @GetMapping
    @Operation(summary = "Search schedule plans with pagination and filters")
    public ResponseEntity<SchedulePlanPageResponse> search(
            @RequestParam(required = false) String medicoId,
            @RequestParam(required = false, name = "specialistId") String legacySpecialistId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) Boolean activeVersion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String resolvedMedicoId = medicoId != null && !medicoId.isBlank() ? medicoId : legacySpecialistId;
        return ResponseEntity.ok(schedulePlanAdminService.search(
                new SchedulePlanSearchCriteria(resolvedMedicoId, startDate, endDate, published, activeVersion, page, size)
        ));
    }

    @GetMapping({"/medicos/{medicoId}", "/specialists/{medicoId}"})
    @Operation(summary = "List schedule plan versions by medico")
    public ResponseEntity<List<SchedulePlanDTO>> listByMedico(
            @PathVariable String medicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(schedulePlanAdminService.listByMedico(medicoId, startDate, endDate));
    }
}
