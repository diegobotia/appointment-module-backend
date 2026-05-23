package com.ipscentir.appointments.application.exception;

import com.ipscentir.appointments.domain.service.FacilityOperatingWindow;

import java.util.UUID;

/**
 * El slot o bloque propuesto excede el horario institucional de la sede.
 */
public class FacilityOperatingHoursViolationException extends RuntimeException {

    private final Integer sedeId;
    private final String sedeNombre;
    private final FacilityOperatingWindow allowedWindow;

    public FacilityOperatingHoursViolationException(
            String message,
            Integer sedeId,
            String sedeNombre,
            FacilityOperatingWindow allowedWindow
    ) {
        super(message);
        this.sedeId = sedeId;
        this.sedeNombre = sedeNombre;
        this.allowedWindow = allowedWindow;
    }

    public Integer sedeId() {
        return sedeId;
    }

    public String sedeNombre() {
        return sedeNombre;
    }

    public FacilityOperatingWindow allowedWindow() {
        return allowedWindow;
    }
}
