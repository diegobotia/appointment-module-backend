package com.ipscentir.appointments.application.dto.availability;

import java.time.LocalDate;
import java.util.List;

public record DoctorDayAvailabilityDTO(
        LocalDate date,
        List<DoctorAvailabilitySlotDTO> slots
) {
}
