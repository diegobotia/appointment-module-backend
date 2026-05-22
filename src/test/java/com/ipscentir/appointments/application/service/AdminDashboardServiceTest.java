package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.admin.DashboardKpiResponse;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import com.ipscentir.appointments.domain.repository.PqrsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PqrsRepository pqrsRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminDashboardService, "maxRetryAttempts", 3);
    }

    @Test
    void getKpisAggregatesAppointmentPqrsAndNotificationCounts() {
        LocalDate today = LocalDate.of(2026, 5, 20);
        when(appointmentRepository.countByAppointmentDate(today)).thenReturn(4L);
        when(appointmentRepository.countByAppointmentDate(today.plusDays(1))).thenReturn(2L);
        when(appointmentRepository.countByAppointmentDateBetween(today, today.plusDays(6))).thenReturn(10L);
        when(appointmentRepository.countByStatus(any(AppointmentStatus.class))).thenReturn(1L);
        when(pqrsRepository.countByStatus(PqrsStatus.CREADO)).thenReturn(3L);
        when(pqrsRepository.countByStatus(PqrsStatus.EN_REVISION)).thenReturn(1L);
        when(notificationRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(2L);
        when(notificationRepository.countRetryable(3)).thenReturn(1L);

        DashboardKpiResponse kpis = adminDashboardService.getKpis(today);

        assertEquals(today, kpis.referenceDate());
        assertEquals(4L, kpis.appointmentsToday());
        assertEquals(2L, kpis.appointmentsTomorrow());
        assertEquals(10L, kpis.appointmentsThisWeek());
        assertEquals(4L, kpis.pqrsOpen());
        assertEquals(2L, kpis.notificationsFailed());
        assertEquals(1L, kpis.notificationsPendingRetry());
    }
}
