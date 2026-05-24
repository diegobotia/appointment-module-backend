package com.ipscentir.appointments.application.dto.availability;

import java.time.LocalDate;
import java.util.List;

public record MedicoAvailabilityResponse(
        String medicoId,
        Integer sedeId,
        LocalDate fromDate,
        LocalDate toDate,
        int totalAvailableSlots,
        List<MedicoDayAvailabilityDTO> days
) {
}
