package com.ipscentir.appointments.application.dto;

import java.util.List;

public record AppointmentPageResponse(
        List<AppointmentDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
