package com.ipscentir.appointments.application.event;

import com.ipscentir.appointments.domain.model.appointment.AppointmentCancelledEvent;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCreatedEvent;
import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import com.ipscentir.appointments.application.service.N8nEventJournalService;
import com.ipscentir.appointments.domain.service.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationProvider notificationProvider;
    private final N8nEventJournalService n8nEventJournalService;

    /**
     * Reacciona únicamente DESPUÉS de que la transacción de base de datos hace COMMIT exitoso.
     * Es ejecutado asíncronamente (@Async) en otro thread para no bloquear la respuesta HTTP.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Received Domain Event: AppointmentCreatedEvent for patient: {}", event.patientId());

        // 1. Compose Notification template (For Sprint 7 Mock, we invent the phone based on Patient Id locally)
        String mockPatientPhone = "+573001234567"; // Podría consultarse a PatientServiceClient en el futuro
        String messageBody = String.format("IPS Centir - Su cita para el %s a las %s tipo %s ha sido agendada.",
                event.appointmentDate().toString(),
                event.appointmentTime().toString(),
                event.appointmentType().name()
        );

        Notification notification = Notification.create(
            event.patientId(),
                NotificationType.SMS,
                mockPatientPhone,
                messageBody
        );

        // 2. Transmit to external boundaries via Twilio Adapter Port
        boolean success = notificationProvider.sendNotification(notification);

        if (success) {
            notification.markAsSent();
        } else {
            notification.markAsFailed("Provider adapter rejected transmission");
        }

        // 3. Save Log in Local Registry Table
        notificationRepository.save(notification);

        // 4. Persist event for n8n/outbox processing
        n8nEventJournalService.recordAppointmentCreated(event);
    }
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Received Domain Event: AppointmentCancelledEvent for appointment: {}", event.appointmentId());

        String mockPatientPhone = "+573001234567";
        String messageBody = String.format("IPS Centir - Lamentamos informarle que su cita el %s fue cancelada. Motivo: %s",
                event.appointmentDate().toString(),
                event.cancellationReason()
        );

        Notification notification = Notification.create(
            event.patientId(),
                NotificationType.WHATSAPP,
                mockPatientPhone,
                messageBody
        );

        boolean success = notificationProvider.sendNotification(notification);

        if (success) {
            notification.markAsSent();
        } else {
            notification.markAsFailed("WhatsApp channel delivery unverified");
        }

        notificationRepository.save(notification);
        n8nEventJournalService.recordAppointmentCancelled(event);
    }
}
