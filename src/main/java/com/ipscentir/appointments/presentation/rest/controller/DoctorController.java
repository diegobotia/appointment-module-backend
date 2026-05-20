package com.ipscentir.appointments.presentation.rest.controller;

import com.ipscentir.appointments.application.dto.availability.DoctorAvailabilityResponse;
import com.ipscentir.appointments.application.service.DoctorApplicationService;
import com.ipscentir.appointments.application.service.dto.DoctorAvailableDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Doctors", description = "Endpoints para consultar médicos y su disponibilidad")
public class DoctorController {

    private final DoctorApplicationService doctorApplicationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'MEDICO', 'FACTURACION')")
    @Operation(summary = "Listar médicos disponibles")
    public ResponseEntity<List<DoctorAvailableDTO>> listAvailableDoctors(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availabilityDate
    ) {
        return ResponseEntity.ok(doctorApplicationService.findAvailableDoctors(specialty, facilityId, availabilityDate));
    }

    @GetMapping("/{doctorId}/availability")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'MEDICO', 'FACTURACION')")
    @Operation(summary = "Obtener disponibilidad de un médico en rango de fechas")
    public ResponseEntity<DoctorAvailabilityResponse> getDoctorAvailability(
            @PathVariable String doctorId,
            @RequestParam @NotNull UUID facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        LocalDate fromDate = from != null ? from : startDate;
        return ResponseEntity.ok(doctorApplicationService.getDoctorAvailability(doctorId, facilityId, fromDate, to));
    }
}
