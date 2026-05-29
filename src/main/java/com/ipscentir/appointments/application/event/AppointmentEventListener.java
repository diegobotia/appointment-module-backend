package com.ipscentir.appointments.application.event;

import com.ipscentir.appointments.application.service.N8nEventJournalService;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCancelledEvent;
import com.ipscentir.appointments.domain.model.appointment.AppointmentCreatedEvent;
import com.ipscentir.appointments.domain.model.appointment.AppointmentRescheduledEvent;
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

    private final N8nEventJournalService n8nEventJournalService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("AppointmentCreatedEvent appointmentId={}", event.appointmentId());
        n8nEventJournalService.recordAppointmentCreated(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("AppointmentCancelledEvent appointmentId={}", event.appointmentId());
        n8nEventJournalService.recordAppointmentCancelled(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentRescheduled(AppointmentRescheduledEvent event) {
        log.info("AppointmentRescheduledEvent appointmentId={} channel={}", event.appointmentId(), event.channel());
        n8nEventJournalService.recordAppointmentRescheduled(event);
    }
}
