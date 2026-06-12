package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.schedule.SchedulePlanDTO;
import com.ipscentir.appointments.application.security.StaffSecurityHelper;
import com.ipscentir.appointments.application.service.SchedulePlanAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/me/schedule-plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEDICO')")
@Tag(name = "My Schedule Plans", description = "Planes trimestrales del médico autenticado")
public class MeSchedulePlanController {

    private final StaffSecurityHelper staffSecurityHelper;
    private final SchedulePlanAdminService schedulePlanAdminService;

    @GetMapping
    @Operation(summary = "Planes propios del médico autenticado",
            description = "Retorna los planes trimestrales del médico. No requiere sedeId porque los planes se filtran por la identidad del médico autenticado.")
    public ResponseEntity<List<SchedulePlanDTO>> getMySchedulePlans(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        String medicoId = staffSecurityHelper.requireDoctorIdForMedico();
        return ResponseEntity.ok(schedulePlanAdminService.listByMedico(medicoId, startDate, endDate));
    }
}
