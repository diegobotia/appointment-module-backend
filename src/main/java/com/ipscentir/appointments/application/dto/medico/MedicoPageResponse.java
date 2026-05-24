package com.ipscentir.appointments.application.dto.medico;

import java.util.List;

public record MedicoPageResponse(
        List<MedicoSummaryDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
