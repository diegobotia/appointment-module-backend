package com.ipscentir.appointments.application.dto.schedule;

import java.util.List;

public record SchedulePlanPageResponse(
        List<SchedulePlanDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
