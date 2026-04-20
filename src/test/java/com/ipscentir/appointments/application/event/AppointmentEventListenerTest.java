package com.ipscentir.appointments.application.event;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.application.service.N8nEventJournalService;
import com.ipscentir.appointments.domain.service.NotificationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentEventListenerTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockBean
    private NotificationProvider notificationProvider;

    @MockBean
    private N8nEventJournalService n8nEventJournalService;

    @Test
    void testAppointmentCreationTriggersNotification() {
        // Arrange
        when(notificationProvider.sendNotification(any())).thenReturn(true);

        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        Appointment appointment = Appointment.scheduleNew(
            patientId, doctorId, null,
            new AppointmentScheduleData(scheduleId, facilityId, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30, AppointmentType.PRESENCIAL, com.ipscentir.appointments.domain.model.appointment.AppointmentStatus.SCHEDULED, "Checkup")
        );

        // Act - Se dispara el guardado en base de datos.
        // Las abstracciones de Spring Data llamarán al getter domainEvents() 
        // e invocarán a TransactionalEventListener(AFTER_COMMIT) en otro hilo de forma Asíncrona.
        appointmentRepository.save(appointment);

        // Assert - Esperar activamente en la línea temporal (max 5 segs) a que el Worker Async notifique a Twilio (Mock)
        verify(notificationProvider, timeout(5000).times(1)).sendNotification(any());
        verify(n8nEventJournalService, timeout(5000).times(1)).recordAppointmentCreated(any());
    }

    @Test
    void testAppointmentCancellationTriggersNotificationAndJournal() {
        when(notificationProvider.sendNotification(any())).thenReturn(true);

        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        Appointment appointment = Appointment.scheduleNew(
            patientId, doctorId, null,
            new AppointmentScheduleData(scheduleId, facilityId, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30, AppointmentType.PRESENCIAL, com.ipscentir.appointments.domain.model.appointment.AppointmentStatus.SCHEDULED, "Checkup")
        );

        appointmentRepository.save(appointment);
        appointment.cancel("No puede asistir");
        appointmentRepository.save(appointment);

        verify(notificationProvider, timeout(5000).atLeastOnce()).sendNotification(any());
        verify(n8nEventJournalService, timeout(5000).times(1)).recordAppointmentCancelled(any());
    }
}
