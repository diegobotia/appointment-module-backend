package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.admin.DashboardKpiResponse;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import com.ipscentir.appointments.domain.repository.PqrsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AppointmentRepository appointmentRepository;
    private final PqrsRepository pqrsRepository;
    private final NotificationRepository notificationRepository;

    @Value("${notifications.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Transactional(readOnly = true)
    public DashboardKpiResponse getKpis(LocalDate referenceDate) {
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekEnd = today.plusDays(6);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (AppointmentStatus status : AppointmentStatus.values()) {
            byStatus.put(status.name(), appointmentRepository.countByStatus(status));
        }

        long pqrsOpen = pqrsRepository.countByStatus(PqrsStatus.CREADO)
                + pqrsRepository.countByStatus(PqrsStatus.EN_REVISION);

        return new DashboardKpiResponse(
                today,
                appointmentRepository.countByAppointmentDate(today),
                appointmentRepository.countByAppointmentDate(tomorrow),
                appointmentRepository.countByAppointmentDateBetween(today, weekEnd),
                byStatus,
                pqrsOpen,
                notificationRepository.countByStatus(NotificationStatus.FAILED),
                notificationRepository.countRetryable(maxRetryAttempts)
        );
    }
}
