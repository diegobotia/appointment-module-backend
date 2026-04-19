package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanBlockRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanDTO;
import com.ipscentir.appointments.application.service.SchedulePlanAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/schedule-plans")
@RequiredArgsConstructor
@Tag(name = "Admin Schedule Plans API", description = "Administrative endpoints for quarterly planning and publication")
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
    @Operation(summary = "Publish schedule plan version")
    public ResponseEntity<SchedulePlanDTO> publish(@PathVariable UUID planId) {
        return ResponseEntity.ok(schedulePlanAdminService.publish(planId));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get schedule plan by id")
    public ResponseEntity<SchedulePlanDTO> getById(@PathVariable UUID planId) {
        return ResponseEntity.ok(schedulePlanAdminService.getById(planId));
    }

    @GetMapping("/specialists/{specialistId}")
    @Operation(summary = "List schedule plan versions by specialist")
    public ResponseEntity<List<SchedulePlanDTO>> listBySpecialist(
            @PathVariable UUID specialistId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer quarter
    ) {
        return ResponseEntity.ok(schedulePlanAdminService.listBySpecialist(specialistId, year, quarter));
    }
}
