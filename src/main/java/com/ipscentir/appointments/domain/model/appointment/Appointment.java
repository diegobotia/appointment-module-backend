package com.ipscentir.appointments.domain.model.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
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

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private String doctorId;

    @Column(nullable = false)
    private UUID facilityId;

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
        if (this.status != AppointmentStatus.SCHEDULED && this.status != AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO) {
            throw new IllegalStateException("Only SCHEDULED or PENDIENTE_CONFIRMACION_GRUPO appointments can be confirmed");
        }
        this.status = AppointmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        
        // Registrar Evento de Dominio opcional si hiciera falta notificar al paciente.
        // La creación de notificaciones la manejamos on-save en el event listener general.
    }

    public void cancel(String reason) {
        if (this.status == AppointmentStatus.CANCELLED || this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Appointment cannot be cancelled from its current state");
        }
        
        if (this.appointmentDate.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot cancel an appointment in the past");
        }

        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;

        // Publicar evento de cancelación
        registerEvent(new AppointmentCancelledEvent(
                this.id, this.patientId, this.doctorId, this.appointmentDate, this.appointmentTime, reason
        ));
    }

    // Factory method wrapper for domain driven design instantiation
        public static Appointment scheduleNew(
            UUID patientId,
            String primaryDoctorId,
            String secondaryDoctorId,
            AppointmentScheduleData scheduleData
        ) {
        UUID appointmentId = UUID.randomUUID();

        if (scheduleData.type() == AppointmentType.JUNTA_MEDICA && secondaryDoctorId == null) {
            throw new IllegalStateException("Junta medica requires exactly 2 specialists");
        }

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patientId(patientId)
                .doctorId(primaryDoctorId)
            .facilityId(scheduleData.facilityId())
            .scheduleId(scheduleData.scheduleId())
            .appointmentDate(scheduleData.date())
            .appointmentTime(scheduleData.time())
            .durationMinutes(scheduleData.duration())
            .appointmentType(scheduleData.type())
            .status(scheduleData.status())
            .reason(scheduleData.reason())
                .build();

        appointment.addParticipant(primaryDoctorId, 1, AppointmentParticipantRole.PRIMARY);
        if (secondaryDoctorId != null) {
            appointment.addParticipant(secondaryDoctorId, 2, AppointmentParticipantRole.SECONDARY);
        }
        
        // Registramos evento para disparar asíncronamente notificaciones (SMS/Email) al guardar.
        appointment.registerEvent(new AppointmentCreatedEvent(
                appointment.id,
                appointment.patientId,
                appointment.doctorId,
                appointment.appointmentDate,
                appointment.appointmentTime,
                appointment.appointmentType
        ));
        return appointment;
    }

    public void transitionGroupPendingToScheduled() {
        if (this.status == AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO) {
            this.status = AppointmentStatus.SCHEDULED;
        }
    }

    public String getSecondaryDoctorId() {
        return this.participants.stream()
                .filter(p -> p.getParticipantRole() == AppointmentParticipantRole.SECONDARY)
                .map(AppointmentParticipant::getDoctorId)
                .findFirst()
                .orElse(null);
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
