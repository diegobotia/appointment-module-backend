package com.ipscentir.appointments.application.dto.admin;

import java.time.LocalDate;
import java.util.Map;

public record DashboardKpiResponse(
        LocalDate referenceDate,
        long appointmentsToday,
        long appointmentsTomorrow,
        long appointmentsThisWeek,
        Map<String, Long> appointmentsByStatus,
        long pqrsOpen,
        long notificationsFailed,
        long notificationsPendingRetry
) {
}
