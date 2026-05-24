package com.ipscentir.appointments.application.dto.integration.n8n;

import java.time.LocalDate;
import java.util.List;

public record N8nPendingRemindersResponse(
        LocalDate date,
        int count,
        List<N8nPatientAppointmentSummaryDTO> appointments,
        String summary
) {}
