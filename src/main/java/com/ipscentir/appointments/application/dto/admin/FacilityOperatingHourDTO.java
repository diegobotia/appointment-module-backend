package com.ipscentir.appointments.application.dto.admin;

import java.time.LocalTime;
import java.util.UUID;

public record FacilityOperatingHourDTO(
        UUID id,
        int dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {
}
