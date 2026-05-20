package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationPurpose;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.NotificationRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final PacienteRepository pacienteRepository;
    private final ContactoRepository contactoRepository;
    private final NotificationDispatchService notificationDispatchService;

    @Value("${notifications.reminder-24h-enabled:true}")
    private boolean reminder24hEnabled;

    @Value("${notifications.reminder-2h-enabled:false}")
    private boolean reminder2hEnabled;

    @Scheduled(cron = "${appointments.reminder.cron:0 0 8 * * *}")
    @Transactional
    public void send24hReminders() {
        if (!reminder24hEnabled) {
            return;
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepository.findByDateAndStatusIn(
                tomorrow,
                List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)
        );

        log.info("24h reminder: {} appointments for {}", appointments.size(), tomorrow);
        for (Appointment appointment : appointments) {
            try {
                sendRemindersForAppointment(appointment, NotificationPurpose.REMINDER_24H);
            } catch (Exception e) {
                log.error("24h reminder error {}: {}", appointment.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${notifications.reminder-2h-cron:0 */15 * * * *}")
    @Transactional
    public void send2hReminders() {
        if (!reminder2hEnabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime windowStart = LocalDateTime.now().plusHours(2).minusMinutes(15);
        LocalDateTime windowEnd = LocalDateTime.now().plusHours(2).plusMinutes(15);

        List<Appointment> appointments = appointmentRepository.findByDateAndStatusIn(
                today,
                List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)
        );

        for (Appointment appointment : appointments) {
            LocalDateTime at = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime());
            if (at.isBefore(windowStart) || at.isAfter(windowEnd)) {
                continue;
            }
            try {
                sendRemindersForAppointment(appointment, NotificationPurpose.REMINDER_2H);
            } catch (Exception e) {
                log.error("2h reminder error {}: {}", appointment.getId(), e.getMessage());
            }
        }
    }

    private void sendRemindersForAppointment(Appointment appointment, NotificationPurpose purpose) {
        List<Notification> existing = notificationRepository.findByEntityId(appointment.getId());

        boolean whatsappSent = existing.stream()
                .anyMatch(n -> n.getPurpose() == purpose
                        && n.getNotificationType() == NotificationType.WHATSAPP
                        && n.getStatus() == NotificationStatus.SENT);

        boolean emailSent = existing.stream()
                .anyMatch(n -> n.getPurpose() == purpose
                        && n.getNotificationType() == NotificationType.EMAIL
                        && n.getStatus() == NotificationStatus.SENT);

        if (whatsappSent && emailSent) {
            return;
        }

        var pacienteOpt = pacienteRepository.findById(appointment.getPatientId());
        if (pacienteOpt.isEmpty()) {
            return;
        }
        var paciente = pacienteOpt.get();
        var contactoOpt = contactoRepository.findById(paciente.getIdContacto());
        if (contactoOpt.isEmpty()) {
            return;
        }
        var contacto = contactoOpt.get();

        if (!whatsappSent && contacto.getTelefono() != null && !contacto.getTelefono().isBlank()) {
            String message = String.format(
                    "IPS Centir - Recordatorio: Hola %s, le recordamos su cita médica el %s a las %s.",
                    paciente.getNombres(),
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime()
            );
            notificationDispatchService.dispatch(Notification.create(
                    appointment.getId(),
                    NotificationType.WHATSAPP,
                    purpose,
                    contacto.getTelefono(),
                    message
            ));
        }

        if (!emailSent && contacto.getEmail() != null && !contacto.getEmail().isBlank()) {
            String message = String.format(
                    "Estimado(a) %s %s, le recordamos su cita el %s a las %s en IPS Centir.",
                    paciente.getNombres(),
                    paciente.getApellidos(),
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime()
            );
            notificationDispatchService.dispatch(Notification.create(
                    appointment.getId(),
                    NotificationType.EMAIL,
                    purpose,
                    contacto.getEmail(),
                    message
            ));
        }
    }
}
