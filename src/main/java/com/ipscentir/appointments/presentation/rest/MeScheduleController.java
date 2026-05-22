package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.schedule.MyScheduleResponse;
import com.ipscentir.appointments.application.service.ScheduleApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEDICO')")
@Tag(name = "My Schedule", description = "Agenda y citas del médico autenticado")
public class MeScheduleController {

    private final ScheduleApplicationService scheduleApplicationService;

    @GetMapping
    @Operation(summary = "Agenda propia: plantillas, citas y disponibilidad contextual")
    public ResponseEntity<MyScheduleResponse> getMySchedule(
            @RequestParam @NotNull Integer sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(scheduleApplicationService.getMySchedule(sedeId, from, to));
    }
}
