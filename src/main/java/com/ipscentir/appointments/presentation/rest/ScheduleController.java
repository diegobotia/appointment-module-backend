package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.AvailableSlotDTO;
import com.ipscentir.appointments.application.dto.ScheduleDTO;
import com.ipscentir.appointments.application.dto.availability.MedicoAvailabilityResponse;
import com.ipscentir.appointments.application.service.ScheduleApplicationService;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules API", description = "Agendas operativas y disponibilidad por sede")
public class ScheduleController {

    private final ScheduleApplicationService scheduleApplicationService;

    @GetMapping({"/medicos/{medicoId}/day/{dayOfWeek}", "/doctors/{medicoId}/day/{dayOfWeek}"})
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO', 'FACTURACION')")
    @Operation(summary = "Plantilla de agenda semanal (día)")
    public ResponseEntity<ScheduleDTO> getMedicoSchedule(
            @PathVariable String medicoId,
            @PathVariable DayOfWeek dayOfWeek,
            @RequestParam @NotNull Integer sedeId
    ) {
        return ResponseEntity.ok(scheduleApplicationService.getScheduleForMedicoAndFacility(medicoId, sedeId, dayOfWeek));
    }

    @GetMapping({"/medicos/{medicoId}/templates", "/doctors/{medicoId}/templates"})
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO', 'FACTURACION')")
    @Operation(summary = "Todas las plantillas activas del médico en una sede")
    public ResponseEntity<List<ScheduleDTO>> listMedicoScheduleTemplates(
            @PathVariable String medicoId,
            @RequestParam @NotNull Integer sedeId
    ) {
        return ResponseEntity.ok(scheduleApplicationService.listScheduleTemplatesForMedico(medicoId, sedeId));
    }

    @GetMapping({"/medicos/{medicoId}/availability", "/doctors/{medicoId}/availability"})
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO', 'FACTURACION')")
    @Operation(summary = "Slots disponibles en una fecha")
    public ResponseEntity<List<AvailableSlotDTO>> getAvailabilityForDate(
            @PathVariable String medicoId,
            @RequestParam @NotNull Integer sedeId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(scheduleApplicationService.getAvailabilityForDate(medicoId, sedeId, date));
    }

    @GetMapping({"/medicos/{medicoId}/availability/range", "/doctors/{medicoId}/availability/range"})
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO', 'FACTURACION')")
    @Operation(summary = "Slots disponibles en rango de fechas")
    public ResponseEntity<MedicoAvailabilityResponse> getAvailabilityInRange(
            @PathVariable String medicoId,
            @RequestParam @NotNull Integer sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(scheduleApplicationService.getAvailabilityInRange(medicoId, sedeId, from, to));
    }
}
