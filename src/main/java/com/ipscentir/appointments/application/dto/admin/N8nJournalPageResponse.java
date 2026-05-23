package com.ipscentir.appointments.application.dto.admin;

import java.util.List;

public record N8nJournalPageResponse(
        List<N8nJournalEntryDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long appointmentsCreatedByN8n,
        long appointmentsCreatedByStaff
) {
}
