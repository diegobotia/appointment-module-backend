package com.ipscentir.appointments.application.dto.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SchedulePlanDTO(
        UUID id,
        String specialistId,
        Integer sedeId,
        LocalDate startDate,
        LocalDate endDate,
        int versionNumber,
        boolean published,
        boolean activeVersion,
        LocalDateTime publishedAt,
        List<SchedulePlanSlotDTO> slots,
        List<SchedulePlanBlockDTO> blocks
) {
}
