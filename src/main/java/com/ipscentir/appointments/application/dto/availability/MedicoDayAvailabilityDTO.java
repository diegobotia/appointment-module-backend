package com.ipscentir.appointments.application.dto.availability;

import java.time.LocalDate;
import java.util.List;

public record MedicoDayAvailabilityDTO(
        LocalDate date,
        List<MedicoAvailabilitySlotDTO> slots
) {
}
