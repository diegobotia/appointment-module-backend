package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentBookingService {

    private final HumanResourceAvailabilityService humanResourceAvailabilityService;
    private final AppointmentRepository appointmentRepository;
    private final ResourceCapacityService resourceCapacityService;
    private final ScheduleRepository scheduleRepository;
    private final TransactionTemplate transactionTemplate;

    public Appointment bookAppointment(AppointmentBookingRequest request) {
        if (transactionTemplate == null) {
            if (isTherapy(request.type())) {
                return bookTherapyAppointment(request);
            }
            return bookRegularAppointment(request);
        }

        Appointment appointment = transactionTemplate.execute(status -> {
            if (isTherapy(request.type())) {
                return bookTherapyAppointment(request);
            }
            return bookRegularAppointment(request);
        });
        if (appointment == null) {
            throw new IllegalStateException("Transaction returned null appointment");
        }
        return appointment;
    }

    /**
     * Cita creada por override administrativo: salta todas las validaciones de horario,
     * agenda, festivos y capacidad. Solo verifica que el doctor no tenga solapamiento
     * y que el paciente no tenga duplicado en la misma fecha.
     */
    public Appointment bookAdminOverrideAppointment(AppointmentBookingRequest request, int durationMinutes) {
        HumanResourceBookingContext overrideContext = new HumanResourceBookingContext(
                request.patientId(),
                request.doctorId(),
                request.resolvedAdditionalDoctorIds(),
                null,
                request.sedeId(),
                request.date(),
                request.time(),
                request.type(),
                durationMinutes
        );
        humanResourceAvailabilityService.assertAdminOverrideAllowed(overrideContext, null);

        Appointment appointment = Appointment.scheduleNew(
                request.patientId(),
                request.doctorId(),
                request.resolvedAdditionalDoctorIds(),
                new AppointmentScheduleData(
                        null,
                        request.sedeId(),
                        request.date(),
                        request.time(),
                        durationMinutes,
                        request.type(),
                        AppointmentStatus.SCHEDULED,
                        request.reason()
                ),
                request.resolvedChannel(),
                request.n8nConversationId()
        );
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancelAppointment(UUID appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        appointment.cancel(reason);
        resourceCapacityService.release(appointmentId);
        Appointment saved = appointmentRepository.save(appointment);

        if (isTherapy(appointment.getAppointmentType())) {
            reEvaluateTherapyGroup(appointment);
        }

        return saved;
    }

    private void reEvaluateTherapyGroup(Appointment appointment) {
        List<Appointment> remaining = appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(
                appointment.getScheduleId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getAppointmentType()
        );
        long activeGroupSize = remaining.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .count();

        if (activeGroupSize < HumanResourceAvailabilityService.THERAPY_GROUP_MIN) {
            remaining.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                    .forEach(Appointment::regressScheduledToGroupPending);
            appointmentRepository.saveAll(remaining);
        }
    }

    public Appointment bookAdministrativeAppointment(AdministrativeAppointmentBookingRequest request) {
        if (transactionTemplate == null) {
            return bookAdministrativeInternal(request);
        }

        Appointment appointment = transactionTemplate.execute(status -> bookAdministrativeInternal(request));
        if (appointment == null) {
            throw new IllegalStateException("Transaction returned null appointment");
        }
        return appointment;
    }

    private Appointment bookAdministrativeInternal(AdministrativeAppointmentBookingRequest request) {
        int duration = request.resolvedDurationMinutes();

        humanResourceAvailabilityService.assertAdministrativeBookingAllowed(
                request.participantDoctorIds(),
                request.sedeId(),
                request.date(),
                request.time(),
                duration
        );

        resourceCapacityService.assertCanAllocate(
                request.sedeId(),
                AppointmentType.STAFF,
                null,
                request.date(),
                request.time(),
                duration,
                null
        );

        Appointment created = Appointment.scheduleStaffMeeting(
                request.participantDoctorIds(),
                new AppointmentScheduleData(
                        null,
                        request.sedeId(),
                        request.date(),
                        request.time(),
                        duration,
                        AppointmentType.STAFF,
                        AppointmentStatus.SCHEDULED,
                        request.reason()
                ),
                BookingChannel.STAFF
        );

        Appointment saved = appointmentRepository.save(created);
        resourceCapacityService.allocate(saved);
        return saved;
    }

    private Appointment bookRegularAppointment(AppointmentBookingRequest request) {
        if (request.type() == AppointmentType.BLOQUEO) {
            return bookBloqueoAppointment(request);
        }

        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        int blockDuration = schedule.getSlotDurationMinutes();
        UUID consultorioId = schedule.getConsultorioId();

        HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                request.patientId(),
                request.doctorId(),
                request.resolvedAdditionalDoctorIds(),
                request.scheduleId(),
                request.sedeId(),
                request.date(),
                request.time(),
                request.type(),
                blockDuration
        );

        humanResourceAvailabilityService.assertBookingAllowed(context);

        resourceCapacityService.assertCanAllocate(
                request.sedeId(),
                request.type(),
                request.scheduleId(),
                request.date(),
                request.time(),
                blockDuration,
                null,
                consultorioId
        );

        Appointment appointment = Appointment.scheduleNew(
                request.patientId(),
                request.doctorId(),
                request.resolvedAdditionalDoctorIds(),
                new AppointmentScheduleData(
                        request.scheduleId(),
                        request.sedeId(),
                        request.date(),
                        request.time(),
                        blockDuration,
                        request.type(),
                        AppointmentStatus.SCHEDULED,
                        request.reason()
                ),
                request.resolvedChannel(),
                request.n8nConversationId()
        );
        Appointment saved = appointmentRepository.save(appointment);
        resourceCapacityService.allocate(saved, consultorioId);
        return saved;
    }

    private Appointment bookBloqueoAppointment(AppointmentBookingRequest request) {
        int duration = 30;

        HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                request.patientId(),
                request.doctorId(),
                request.resolvedAdditionalDoctorIds(),
                null,
                request.sedeId(),
                request.date(),
                request.time(),
                request.type(),
                duration
        );

        humanResourceAvailabilityService.assertBookingAllowed(context);

        Appointment appointment = Appointment.scheduleNew(
                request.patientId(),
                request.doctorId(),
                request.resolvedAdditionalDoctorIds(),
                new AppointmentScheduleData(
                        null,
                        request.sedeId(),
                        request.date(),
                        request.time(),
                        duration,
                        request.type(),
                        AppointmentStatus.SCHEDULED,
                        request.reason()
                ),
                request.resolvedChannel(),
                request.n8nConversationId()
        );
        return appointmentRepository.save(appointment);
    }

    private Appointment bookTherapyAppointment(AppointmentBookingRequest request) {
        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        int blockDuration = schedule.getSlotDurationMinutes();
        UUID consultorioId = schedule.getConsultorioId();

        HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                request.patientId(),
                request.doctorId(),
                request.resolvedAdditionalDoctorIds(),
                request.scheduleId(),
                request.sedeId(),
                request.date(),
                request.time(),
                request.type(),
                blockDuration
        );

        humanResourceAvailabilityService.assertBookingAllowed(context);
        humanResourceAvailabilityService.assertTherapyGroupAllowsNewPatient(
                request.scheduleId(),
                request.date(),
                request.time(),
                request.type()
        );

        List<Appointment> slotAppointments = appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(
                request.scheduleId(),
                request.date(),
                request.time(),
                request.type()
        );
        long activeGroupSize = slotAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .count();

        resourceCapacityService.assertCanAllocate(
                request.sedeId(),
                request.type(),
                request.scheduleId(),
                request.date(),
                request.time(),
                blockDuration,
                null,
                consultorioId
        );

        AppointmentStatus initialStatus = activeGroupSize + 1 < HumanResourceAvailabilityService.THERAPY_GROUP_MIN
                ? AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO
                : AppointmentStatus.SCHEDULED;

        Appointment appointment = Appointment.scheduleNew(
                request.patientId(),
                request.doctorId(),
                request.resolvedAdditionalDoctorIds(),
                new AppointmentScheduleData(
                        request.scheduleId(),
                        request.sedeId(),
                        request.date(),
                        request.time(),
                        blockDuration,
                        request.type(),
                        initialStatus,
                        request.reason()
                ),
                request.resolvedChannel(),
                request.n8nConversationId()
        );

        Appointment saved = appointmentRepository.save(appointment);

        resourceCapacityService.allocate(saved, consultorioId);

        List<Appointment> refreshed = appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(
                request.scheduleId(),
                request.date(),
                request.time(),
                request.type()
        );

        long refreshedActiveGroupSize = refreshed.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .count();

        if (refreshedActiveGroupSize >= HumanResourceAvailabilityService.THERAPY_GROUP_MIN) {
            refreshed.forEach(Appointment::transitionGroupPendingToScheduled);
            appointmentRepository.saveAll(refreshed);
        }

        return saved;
    }

    private boolean isTherapy(AppointmentType type) {
        return type == AppointmentType.TERAPIA_FISICA || type == AppointmentType.TERAPIA_OCUPACIONAL;
    }
}
