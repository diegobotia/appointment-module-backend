package com.ipscentir.appointments.domain.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AdministrativeAppointmentBookingRequest(
        List<String> participantDoctorIds,
        Integer sedeId,
        LocalDate date,
        LocalTime time,
        int durationMinutes,
        String reason
) {
    public static final int DEFAULT_DURATION_MINUTES = 30;

    public int resolvedDurationMinutes() {
        return durationMinutes > 0 ? durationMinutes : DEFAULT_DURATION_MINUTES;
    }
}
