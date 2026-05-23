package com.ipscentir.appointments.application.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleDTO(
        UUID id,
        String doctorId,
        Integer sedeId,
        String specialty,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Integer maxPatientsPerSlot,
        Boolean isActive
) {}
