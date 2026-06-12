package com.ipscentir.appointments.application.dto.admin;

import com.ipscentir.appointments.domain.service.FacilityOperatingWindow;

import java.time.LocalDateTime;


public record FacilityHoursViolationErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp,
        Integer sedeId,
        String sedeNombre,
        FacilityOperatingWindow allowedWindow
) {
}
