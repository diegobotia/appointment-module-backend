package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TherapyPendingGroupCutoffServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Test
    void shouldCancelPendingTherapyWhenPolicyIsRescheduleAndCutoffReached() {
        TherapyPendingGroupCutoffService service = new TherapyPendingGroupCutoffService(
                appointmentRepository,
                "RESCHEDULE",
                120
        );

        LocalDateTime now = LocalDateTime.now();
        Appointment pending = buildPendingTherapy(now.plusMinutes(45));

        when(appointmentRepository.findByStatusAndAppointmentTypeIn(eq(AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO), any()))
                .thenReturn(List.of(pending));
        when(appointmentRepository.findByScheduleAndDateAndTimeAndType(
                pending.getScheduleId(),
                pending.getAppointmentDate(),
                pending.getAppointmentTime(),
                pending.getAppointmentType()
        )).thenReturn(List.of(pending));

        service.processPendingTherapySlots(now);

        assertEquals(AppointmentStatus.CANCELLED, pending.getStatus());
        verify(appointmentRepository).save(pending);
    }

    @Test
    void shouldConfirmPendingTherapyWhenPolicyIsForceConfirmation() {
        TherapyPendingGroupCutoffService service = new TherapyPendingGroupCutoffService(
                appointmentRepository,
                "FORCE_CONFIRMATION",
                120
        );

        LocalDateTime now = LocalDateTime.now();
        Appointment pending = buildPendingTherapy(now.plusMinutes(30));

        when(appointmentRepository.findByStatusAndAppointmentTypeIn(eq(AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO), any()))
                .thenReturn(List.of(pending));
        when(appointmentRepository.findByScheduleAndDateAndTimeAndType(
                pending.getScheduleId(),
                pending.getAppointmentDate(),
                pending.getAppointmentTime(),
                pending.getAppointmentType()
        )).thenReturn(List.of(pending));

        service.processPendingTherapySlots(now);

        assertEquals(AppointmentStatus.CONFIRMED, pending.getStatus());
        verify(appointmentRepository).save(pending);
    }

    @Test
    void shouldSkipAppointmentsBeforeCutoffWindow() {
        TherapyPendingGroupCutoffService service = new TherapyPendingGroupCutoffService(
                appointmentRepository,
                "CANCEL",
                120
        );

        LocalDateTime now = LocalDateTime.now();
        Appointment pending = buildPendingTherapy(now.plusHours(6));

        when(appointmentRepository.findByStatusAndAppointmentTypeIn(eq(AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO), any()))
                .thenReturn(List.of(pending));

        service.processPendingTherapySlots(now);

        assertEquals(AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO, pending.getStatus());
        verify(appointmentRepository, never()).save(any());
    }

    private Appointment buildPendingTherapy(LocalDateTime appointmentDateTime) {
        return Appointment.scheduleNew(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                LocalDate.from(appointmentDateTime),
                LocalTime.from(appointmentDateTime),
                30,
                AppointmentType.TERAPIA_FISICA,
                AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO,
                "Sesion grupal"
        );
    }
}
