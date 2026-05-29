package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.admin.DashboardKpiResponse;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.PqrsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void getKpisAggregatesAppointmentAndPqrsCounts() {
        LocalDate today = LocalDate.of(2026, 5, 20);
        when(appointmentRepository.countByAppointmentDate(today)).thenReturn(4L);
        when(appointmentRepository.countByAppointmentDate(today.plusDays(1))).thenReturn(2L);
        when(appointmentRepository.countByAppointmentDateBetween(today, today.plusDays(6))).thenReturn(10L);
        when(appointmentRepository.countByStatus(any(AppointmentStatus.class))).thenReturn(1L);
        when(pqrsRepository.countByStatus(PqrsStatus.CREADO)).thenReturn(3L);
        when(pqrsRepository.countByStatus(PqrsStatus.EN_REVISION)).thenReturn(1L);

        DashboardKpiResponse kpis = adminDashboardService.getKpis(today);

        assertEquals(today, kpis.referenceDate());
        assertEquals(4L, kpis.appointmentsToday());
        assertEquals(2L, kpis.appointmentsTomorrow());
        assertEquals(10L, kpis.appointmentsThisWeek());
        assertEquals(4L, kpis.pqrsOpen());
    }
}
