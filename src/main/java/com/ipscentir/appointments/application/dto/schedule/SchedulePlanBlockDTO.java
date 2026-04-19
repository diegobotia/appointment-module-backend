package com.ipscentir.appointments.application.dto.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SchedulePlanBlockDTO(
        UUID id,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String reason,
        UUID createdBy
) {
}
