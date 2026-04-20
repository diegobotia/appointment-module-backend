package com.ipscentir.appointments.application.dto.integration.n8n;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record N8nPatientAvailabilityResponse(
        UUID facilityId,
        String serviceType,
        String specialty,
        LocalDate fromDate,
        int limit,
        int availableSlotsCount,
        List<N8nAvailabilitySlotDTO> slots,
        String summary
) {
}
