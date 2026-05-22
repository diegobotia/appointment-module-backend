package com.ipscentir.appointments.domain.service;

import java.time.LocalTime;

/**
 * Ventana horaria permitida de una sede para un día de la semana.
 */
public record FacilityOperatingWindow(
        int dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {
}
