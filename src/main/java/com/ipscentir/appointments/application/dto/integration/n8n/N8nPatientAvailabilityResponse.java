package com.ipscentir.appointments.application.dto.integration.n8n;

import com.ipscentir.appointments.application.dto.AvailableSlotDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record N8nPatientAvailabilityResponse(
        UUID doctorId,
        LocalDate date,
        int availableSlotsCount,
        List<AvailableSlotDTO> slots,
        String summary
) {}