package com.ipscentir.appointments.presentation.rest.controller;

import com.ipscentir.appointments.application.service.DoctorApplicationService;
import com.ipscentir.appointments.application.service.dto.DoctorAvailableDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DoctorController - Endpoints para consultar médicos y disponibilidad
 */
@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Doctors", description = "Endpoints para consultar médicos y su disponibilidad")
public class DoctorController {

    private final DoctorApplicationService doctorApplicationService;

    @GetMapping
    @Operation(
        summary = "Listar médicos disponibles",
        description = "Obtiene lista de médicos con sus especialidades y disponibilidad"
    )
    public ResponseEntity<List<DoctorAvailableDTO>> listAvailableDoctors(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) LocalDate availabilityDate
    ) {
        log.info("Listando médicos: specialty={}, facility_id={}, date={}", specialty, facilityId, availabilityDate);

        List<DoctorAvailableDTO> doctors = doctorApplicationService.findAvailableDoctors(
            specialty, facilityId, availabilityDate
        );

        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/{doctorId}/availability")
    @Operation(
        summary = "Obtener disponibilidad de un médico",
        description = "Devuelve slots disponibles para un médico en la próxima semana"
    )
    public ResponseEntity<?> getDoctorAvailability(
            @PathVariable String doctorId,
            @RequestParam(required = false) LocalDate startDate
    ) {
        log.info("Obteniendo disponibilidad para médico: {}", doctorId);

        return ResponseEntity.ok(doctorApplicationService.getDoctorAvailability(doctorId, startDate));
    }
}
