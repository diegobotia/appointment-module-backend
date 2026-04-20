package com.ipscentir.appointments.application.dto.integration.n8n;

import java.time.LocalDate;
import java.util.List;

public record N8nPatientAvailabilityResponse(
        N8nFacilityId facilityId,
        String serviceType,
        String specialty,
        LocalDate fromDate,
        int limit,
        int availableSlotsCount,
        List<N8nAvailabilitySlotDTO> slots,
        String summary
) {
}
