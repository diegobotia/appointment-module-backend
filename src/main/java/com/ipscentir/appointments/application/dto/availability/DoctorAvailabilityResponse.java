package com.ipscentir.appointments.application.dto.availability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DoctorAvailabilityResponse(
        String doctorId,
        UUID facilityId,
        LocalDate fromDate,
        LocalDate toDate,
        int totalAvailableSlots,
        List<DoctorDayAvailabilityDTO> days
) {
}
