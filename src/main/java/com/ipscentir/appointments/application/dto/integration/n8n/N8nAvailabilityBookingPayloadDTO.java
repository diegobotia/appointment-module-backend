package com.ipscentir.appointments.application.dto.integration.n8n;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record N8nAvailabilityBookingPayloadDTO(
        UUID patientId,
        UUID doctorId,
        N8nFacilityId facilityId,
        UUID secondaryDoctorId,
        UUID scheduleId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String appointmentType,
        String serviceType,
        String specialty,
        Integer durationMinutes,
        Integer availableSeats,
        String reason
) {
}
