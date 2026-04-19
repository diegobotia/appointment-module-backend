package com.ipscentir.appointments.application.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleDTO(
        UUID id,
        UUID doctorId,
        UUID facilityId,
        String specialty,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Integer maxPatientsPerSlot,
        Boolean isActive
) {}
