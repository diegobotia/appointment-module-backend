package com.ipscentir.appointments.application.dto.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record SchedulePlanSlotDTO(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        int maxPatientsPerSlot,
        boolean active
) {
}
