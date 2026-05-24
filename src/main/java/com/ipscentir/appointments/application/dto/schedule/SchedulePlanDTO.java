package com.ipscentir.appointments.application.dto.schedule;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SchedulePlanDTO(
        UUID id,
        @JsonAlias("specialistId")
        String medicoId,
        int planYear,
        int planQuarter,
        int versionNumber,
        boolean published,
        boolean activeVersion,
        LocalDateTime publishedAt,
        List<SchedulePlanSlotDTO> slots,
        List<SchedulePlanBlockDTO> blocks
) {
}
