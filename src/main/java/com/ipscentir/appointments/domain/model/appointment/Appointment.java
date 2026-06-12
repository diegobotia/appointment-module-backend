package com.ipscentir.appointments.domain.model.appointment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "appointments", schema = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Appointment extends AbstractAggregateRoot<Appointment> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = true)
    private UUID patientId;

    @Column(nullable = false)
    private String doctorId;

    @Column(name = "sede_id", nullable = false)
    private Integer sedeId;

    private UUID scheduleId;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentType appointmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_channel", nullable = false)
    @Builder.Default
    private BookingChannel bookingChannel = BookingChannel.STAFF;

    @Column(name = "n8n_conversation_id")
    private String n8nConversationId;

    private String reason;
    private String notes;

    @Column(updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(insertable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<AppointmentParticipant> participants = new ArrayList<>();

    public void confirm() {
        if (this.status != AppointmentStatus.SCHEDULED
                && this.status != AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO) {
            throw new IllegalStateException(
                    "Only SCHEDULED or PENDIENTE_CONFIRMACION_GRUPO appointments can be confirmed");
        }
        this.status = AppointmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();

        // Registrar Evento de Dominio opcional si hiciera falta notificar al paciente.
        // La creación de notificaciones la manejamos on-save en el event listener
        // general.
    }

    public void cancel(String reason) {
        if (this.status == AppointmentStatus.CANCELLED
                || this.status == AppointmentStatus.COMPLETED
                || this.status == AppointmentStatus.NO_SHOW) {
            throw new IllegalStateException("Appointment cannot be cancelled from its current state");
        }

        LocalDateTime appointmentDateTime = LocalDateTime.of(this.appointmentDate, this.appointmentTime);
        if (!appointmentDateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot cancel an appointment that has already passed");
        }

        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;

        // Publicar evento de cancelación
        registerEvent(new AppointmentCancelledEvent(
                this.id, this.patientId, this.doctorId, this.appointmentDate, this.appointmentTime, reason));
    }

    public static Appointment scheduleNew(
            UUID patientId,
            String primaryDoctorId,
            List<String> additionalDoctorIds,
            AppointmentScheduleData scheduleData) {
        return scheduleNew(patientId, primaryDoctorId, additionalDoctorIds, scheduleData, BookingChannel.STAFF, null);
    }

    public static Appointment scheduleNew(
            UUID patientId,
            String primaryDoctorId,
            List<String> additionalDoctorIds,
            AppointmentScheduleData scheduleData,
            BookingChannel bookingChannel,
            String n8nConversationId) {
        UUID appointmentId = UUID.randomUUID();

        if (scheduleData.type() == AppointmentType.JUNTA_MEDICA) {
            List<String> nonNullAdditional = additionalDoctorIds != null ? additionalDoctorIds : Collections.emptyList();
            if (nonNullAdditional.isEmpty() || nonNullAdditional.size() > 3) {
                throw new IllegalStateException("Junta medica requires between 2 and 4 specialists");
            }
        }

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patientId(patientId)
                .doctorId(primaryDoctorId)
                .sedeId(scheduleData.sedeId())
                .scheduleId(scheduleData.scheduleId())
                .appointmentDate(scheduleData.date())
                .appointmentTime(scheduleData.time())
                .durationMinutes(scheduleData.duration())
                .appointmentType(scheduleData.type())
                .status(scheduleData.status())
                .reason(scheduleData.reason())
                .bookingChannel(bookingChannel != null ? bookingChannel : BookingChannel.STAFF)
                .n8nConversationId(n8nConversationId)
                .build();

        appointment.addParticipant(primaryDoctorId, 1, AppointmentParticipantRole.PRIMARY);
        List<String> additional = additionalDoctorIds != null ? additionalDoctorIds : Collections.emptyList();
        for (int i = 0; i < additional.size(); i++) {
            AppointmentParticipantRole role = switch (i) {
                case 0 -> AppointmentParticipantRole.SECONDARY;
                case 1 -> AppointmentParticipantRole.TERTIARY;
                case 2 -> AppointmentParticipantRole.QUATERNARY;
                default -> AppointmentParticipantRole.SECONDARY;
            };
            appointment.addParticipant(additional.get(i), i + 2, role);
        }

        appointment.registerEvent(new AppointmentCreatedEvent(
                appointment.id,
                appointment.patientId,
                appointment.doctorId,
                appointment.appointmentDate,
                appointment.appointmentTime,
                appointment.appointmentType,
                appointment.bookingChannel,
                appointment.n8nConversationId));
        return appointment;
    }

    /**
     * Agenda una reunión o bloqueo interno entre personal administrativo (sin paciente).
     */
    public static Appointment scheduleStaffMeeting(
            List<String> participantDoctorIds,
            AppointmentScheduleData scheduleData,
            BookingChannel bookingChannel
    ) {
        if (participantDoctorIds == null || participantDoctorIds.isEmpty()) {
            throw new IllegalStateException("Junta staff requiere al menos un participante");
        }
        if (scheduleData.type() != AppointmentType.STAFF) {
            throw new IllegalStateException("scheduleStaffMeeting requiere appointmentType STAFF");
        }

        String primaryDoctorId = participantDoctorIds.getFirst();
        UUID appointmentId = UUID.randomUUID();

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patientId(null)
                .doctorId(primaryDoctorId)
                .sedeId(scheduleData.sedeId())
                .scheduleId(scheduleData.scheduleId())
                .appointmentDate(scheduleData.date())
                .appointmentTime(scheduleData.time())
                .durationMinutes(scheduleData.duration())
                .appointmentType(AppointmentType.STAFF)
                .status(scheduleData.status())
                .reason(scheduleData.reason())
                .bookingChannel(bookingChannel != null ? bookingChannel : BookingChannel.STAFF)
                .build();

        for (int i = 0; i < participantDoctorIds.size(); i++) {
            AppointmentParticipantRole role = switch (i) {
                case 0 -> AppointmentParticipantRole.PRIMARY;
                case 1 -> AppointmentParticipantRole.SECONDARY;
                case 2 -> AppointmentParticipantRole.TERTIARY;
                case 3 -> AppointmentParticipantRole.QUATERNARY;
                default -> AppointmentParticipantRole.SECONDARY;
            };
            appointment.addParticipant(participantDoctorIds.get(i), i + 1, role);
        }

        appointment.registerEvent(new AppointmentCreatedEvent(
                appointment.id,
                appointment.patientId,
                appointment.doctorId,
                appointment.appointmentDate,
                appointment.appointmentTime,
                appointment.appointmentType,
                appointment.bookingChannel,
                null
        ));
        return appointment;
    }

    public boolean isAdministrative() {
        return this.appointmentType == AppointmentType.STAFF;
    }

    public boolean isBloqueo() {
        return this.appointmentType == AppointmentType.BLOQUEO;
    }

    public void checkIn() {
        if (isAdministrative()) {
            throw new IllegalStateException("Las citas administrativas no admiten check-in de paciente");
        }
        if (isBloqueo()) {
            throw new IllegalStateException("Las citas tipo BLOQUEO no admiten check-in de paciente");
        }
        if (this.status != AppointmentStatus.SCHEDULED && this.status != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Only SCHEDULED or CONFIRMED appointments can be checked in");
        }
        this.status = AppointmentStatus.CHECKED_IN;
    }

    public void markNoShow() {
        if (this.status == AppointmentStatus.CANCELLED
                || this.status == AppointmentStatus.COMPLETED
                || this.status == AppointmentStatus.NO_SHOW) {
            throw new IllegalStateException("Appointment cannot be marked as no-show from its current state");
        }
        this.status = AppointmentStatus.NO_SHOW;
    }

    public void complete() {
        if (this.status != AppointmentStatus.CHECKED_IN && this.status != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Only CHECKED_IN or CONFIRMED appointments can be completed");
        }
        this.status = AppointmentStatus.COMPLETED;
    }

    public void reschedule(
            LocalDate newDate,
            LocalTime newTime,
            UUID newScheduleId,
            String newDoctorId,
            Integer newSedeId,
            BookingChannel channel,
            String conversationId) {
        if (this.status == AppointmentStatus.CANCELLED
                || this.status == AppointmentStatus.COMPLETED
                || this.status == AppointmentStatus.NO_SHOW
                || this.status == AppointmentStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot reschedule an appointment in its current state");
        }
        if (newDate.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot reschedule to a date in the past");
        }

        LocalDate previousDate = this.appointmentDate;
        LocalTime previousTime = this.appointmentTime;

        this.appointmentDate = newDate;
        this.appointmentTime = newTime;
        this.scheduleId = newScheduleId;
        this.doctorId = newDoctorId;
        this.sedeId = newSedeId;

        if (this.status == AppointmentStatus.CONFIRMED) {
            this.status = AppointmentStatus.SCHEDULED;
            this.confirmedAt = null;
        }

        registerEvent(new AppointmentRescheduledEvent(
                this.id,
                this.patientId,
                this.doctorId,
                previousDate,
                previousTime,
                newDate,
                newTime,
                channel != null ? channel : BookingChannel.STAFF,
                conversationId));
    }

    public boolean isAssignedToDoctor(String doctorId) {
        if (doctorId == null) {
            return false;
        }
        if (doctorId.equals(this.doctorId)) {
            return true;
        }
        return this.participants.stream().anyMatch(p -> doctorId.equals(p.getDoctorId()));
    }

    public void transitionGroupPendingToScheduled() {
        if (this.status == AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO) {
            this.status = AppointmentStatus.SCHEDULED;
        }
    }

    public void regressScheduledToGroupPending() {
        if (this.status == AppointmentStatus.SCHEDULED) {
            this.status = AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO;
        }
    }

    public List<String> getAdditionalDoctorIds() {
        return this.participants.stream()
                .filter(p -> p.getParticipantRole() != AppointmentParticipantRole.PRIMARY)
                .map(AppointmentParticipant::getDoctorId)
                .toList();
    }

    private void addParticipant(String doctorId, int order, AppointmentParticipantRole role) {
        AppointmentParticipant participant = AppointmentParticipant.builder()
                .doctorId(doctorId)
                .participantOrder(order)
                .participantRole(role)
                .build();
        participant.attachToAppointment(this);
        this.participants.add(participant);
        this.participants.sort(Comparator.comparingInt(AppointmentParticipant::getParticipantOrder));
    }
}
