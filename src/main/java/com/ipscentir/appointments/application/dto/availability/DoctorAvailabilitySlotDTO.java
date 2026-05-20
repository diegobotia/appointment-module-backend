package com.ipscentir.appointments.application.dto.availability;

import java.time.LocalTime;

public record DoctorAvailabilitySlotDTO(
        LocalTime time,
        int durationMinutes,
        int availableSeats
) {
}
