package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TherapyPendingGroupCutoffService {

    private final AppointmentRepository appointmentRepository;
    private final TherapyUnderMinPolicy underMinPolicy;
    private final long cutoffMinutes;

    public TherapyPendingGroupCutoffService(
            AppointmentRepository appointmentRepository,
            @Value("${appointments.therapy.pending-group-policy:RESCHEDULE}") String underMinPolicy,
            @Value("${appointments.therapy.pending-group-cutoff-minutes:120}") long cutoffMinutes
    ) {
        this.appointmentRepository = appointmentRepository;
        this.underMinPolicy = TherapyUnderMinPolicy.fromConfig(underMinPolicy);
        this.cutoffMinutes = cutoffMinutes;
    }

    @Scheduled(cron = "${appointments.therapy.pending-group-cutoff-cron:0 */5 * * * *}")
    @Transactional
    public void processPendingTherapySlots() {
        processPendingTherapySlots(LocalDateTime.now());
    }

    void processPendingTherapySlots(LocalDateTime now) {
        List<AppointmentType> therapyTypes = List.of(AppointmentType.TERAPIA_FISICA, AppointmentType.TERAPIA_OCUPACIONAL);
        List<Appointment> pendingAppointments = appointmentRepository.findByStatusAndAppointmentTypeIn(
                AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO,
                therapyTypes
        );

        Map<SlotKey, List<Appointment>> dueSlots = pendingAppointments.stream()
                .filter(appointment -> isDueForCutoff(appointment, now))
                .collect(Collectors.groupingBy(SlotKey::from));

        for (Map.Entry<SlotKey, List<Appointment>> slotEntry : dueSlots.entrySet()) {
            SlotKey slotKey = slotEntry.getKey();
            List<Appointment> slotAppointments = appointmentRepository.findByScheduleAndDateAndTimeAndType(
                    slotKey.scheduleId(),
                    slotKey.date(),
                    slotKey.time(),
                    slotKey.type()
            );

            long activeGroupSize = slotAppointments.stream()
                    .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
                    .filter(appointment -> appointment.getStatus() != AppointmentStatus.NO_SHOW)
                    .count();

            if (activeGroupSize >= com.ipscentir.appointments.domain.service.HumanResourceAvailabilityService.THERAPY_GROUP_MIN) {
                continue;
            }

            applyUnderMinPolicy(slotEntry.getValue(), activeGroupSize);
        }
    }

    private boolean isDueForCutoff(Appointment appointment, LocalDateTime now) {
        LocalDateTime appointmentDateTime = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime());
        LocalDateTime cutoffDateTime = appointmentDateTime.minusMinutes(cutoffMinutes);

        if (appointmentDateTime.isAfter(now)) {
            return !cutoffDateTime.isAfter(now);
        }

        return false;
    }

    private void applyUnderMinPolicy(List<Appointment> pendingAppointments, long activeGroupSize) {
        for (Appointment appointment : pendingAppointments) {
            switch (underMinPolicy) {
                case CANCEL -> {
                    appointment.cancel("Terapia grupal cancelada: no se alcanzo el minimo de " + com.ipscentir.appointments.domain.service.HumanResourceAvailabilityService.THERAPY_GROUP_MIN + " pacientes.");
                    appointmentRepository.save(appointment);
                }
                case RESCHEDULE -> {
                    LocalDate newDate = appointment.getAppointmentDate().plusDays(7);
                    appointment.reschedule(
                            newDate,
                            appointment.getAppointmentTime(),
                            appointment.getScheduleId(),
                            appointment.getDoctorId(),
                            appointment.getSedeId(),
                            BookingChannel.STAFF,
                            null
                    );
                    appointmentRepository.save(appointment);
                    log.info("Rescheduled pending therapy appointment={} from {} to {}",
                            appointment.getId(), appointment.getAppointmentDate(), newDate);
                }
                case FORCE_CONFIRMATION -> {
                    appointment.confirm();
                    appointmentRepository.save(appointment);
                }
                case KEEP_PENDING -> log.info(
                        "Keeping pending therapy appointment={} schedule={} date={} time={} activeGroupSize={}",
                        appointment.getId(),
                        appointment.getScheduleId(),
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime(),
                        activeGroupSize
                );
            }
        }
    }

    private record SlotKey(
            java.util.UUID scheduleId,
            java.time.LocalDate date,
            LocalTime time,
            AppointmentType type
    ) {
        static SlotKey from(Appointment appointment) {
            return new SlotKey(
                    Objects.requireNonNull(appointment.getScheduleId(), "scheduleId is required for therapy appointment"),
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime(),
                    appointment.getAppointmentType()
            );
        }
    }
}
