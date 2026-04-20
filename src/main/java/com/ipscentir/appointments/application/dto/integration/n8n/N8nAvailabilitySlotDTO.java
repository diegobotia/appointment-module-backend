package com.ipscentir.appointments.application.dto.integration.n8n;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record N8nAvailabilitySlotDTO(
        UUID scheduleId,
        UUID doctorId,
        UUID facilityId,
        String serviceType,
        String specialty,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        Integer durationMinutes,
        String appointmentType,
        Integer availableSeats,
        N8nAvailabilityBookingPayloadDTO bookingPayload
) {
}
