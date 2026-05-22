package com.ipscentir.appointments.application.event;

import com.ipscentir.appointments.application.service.N8nEventJournalService;
import com.ipscentir.appointments.application.service.NotificationDispatchService;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCancelledEvent;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCreatedEvent;
import com.ipscentir.appointments.domain.model.appointment.AppointmentRescheduledEvent;
import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationPurpose;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final NotificationDispatchService notificationDispatchService;
    private final N8nEventJournalService n8nEventJournalService;
    private final PacienteRepository pacienteRepository;
    private final ContactoRepository contactoRepository;

    @Value("${notifications.created-alert-enabled:false}")
    private boolean createdAlertEnabled;

    @Value("${notifications.cancelled-alert-enabled:false}")
    private boolean cancelledAlertEnabled;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("AppointmentCreatedEvent appointmentId={}", event.appointmentId());

        String patientPhone = resolvePatientPhone(event.patientId());
        String messageBody = String.format(
                "IPS Centir - Su cita para el %s a las %s tipo %s ha sido agendada.",
                event.appointmentDate(),
                event.appointmentTime(),
                event.appointmentType().name()
        );

        Notification notification = Notification.create(
                event.appointmentId(),
                NotificationType.SMS,
                NotificationPurpose.APPOINTMENT_CREATED,
                patientPhone,
                messageBody
        );

        if (createdAlertEnabled) {
            notificationDispatchService.dispatch(notification);
        } else {
            log.info("Creation alert disabled; skipping SMS for appointment {}", event.appointmentId());
        }

        n8nEventJournalService.recordAppointmentCreated(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("AppointmentCancelledEvent appointmentId={}", event.appointmentId());

        String patientPhone = resolvePatientPhone(event.patientId());
        String messageBody = String.format(
                "IPS Centir - Lamentamos informarle que su cita el %s fue cancelada. Motivo: %s",
                event.appointmentDate(),
                event.cancellationReason()
        );

        Notification notification = Notification.create(
                event.appointmentId(),
                NotificationType.WHATSAPP,
                NotificationPurpose.APPOINTMENT_CANCELLED,
                patientPhone,
                messageBody
        );

        if (cancelledAlertEnabled) {
            notificationDispatchService.dispatch(notification);
        } else {
            log.info("Cancellation alert disabled; skipping WhatsApp for appointment {}", event.appointmentId());
        }

        n8nEventJournalService.recordAppointmentCancelled(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentRescheduled(AppointmentRescheduledEvent event) {
        log.info("AppointmentRescheduledEvent appointmentId={} channel={}", event.appointmentId(), event.channel());
        n8nEventJournalService.recordAppointmentRescheduled(event);
    }

    private String resolvePatientPhone(java.util.UUID patientId) {
        String fallback = "+573001234567";
        try {
            return pacienteRepository.findById(patientId)
                    .flatMap(p -> contactoRepository.findById(p.getIdContacto()))
                    .map(c -> c.getTelefono())
                    .filter(t -> t != null && !t.isBlank())
                    .orElse(fallback);
        } catch (Exception e) {
            log.warn("Could not resolve patient phone: {}", e.getMessage());
            return fallback;
        }
    }
}
