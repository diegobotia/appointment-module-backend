package com.ipscentir.appointments.domain.model.schedule;

import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AvailableSlotDetail(
        UUID scheduleId,
        String doctorId,
        UUID facilityId,
        AppointmentServiceType serviceType,
        String specialty,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        Integer durationMinutes,
        Integer availableSeats
) {
}
