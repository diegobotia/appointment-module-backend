package com.ipscentir.appointments.application.dto.admin;

import com.ipscentir.appointments.application.dto.PqrsDTO;

import java.util.List;

public record PqrsPageResponse(
        List<PqrsDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
