package com.ipscentir.appointments.application.dto.availability;

import java.time.LocalTime;

public record MedicoAvailabilitySlotDTO(
        LocalTime time,
        int durationMinutes,
        int availableSeats
) {
}
