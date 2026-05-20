package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.admin.DashboardKpiResponse;
import com.ipscentir.appointments.application.service.AdminDashboardService;
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

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMINISTRACION')")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard API", description = "KPIs operativos del panel administrativo")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/kpis")
    @Operation(summary = "KPIs de citas, PQRS y notificaciones")
    public ResponseEntity<DashboardKpiResponse> getKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(adminDashboardService.getKpis(date));
    }
}
