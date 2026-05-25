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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class AppointmentBookingService {

    private static final ConcurrentHashMap<String, ReentrantLock> THERAPY_SLOT_LOCKS = new ConcurrentHashMap<>();

    private final HumanResourceAvailabilityService humanResourceAvailabilityService;
    private final AppointmentRepository appointmentRepository;
    private final ResourceCapacityService resourceCapacityService;
    private final ScheduleRepository scheduleRepository;
    private final TransactionTemplate transactionTemplate;

    public Appointment bookAppointment(AppointmentBookingRequest request) {
        if (isTherapy(request.type())) {
            return bookTherapyUnderLock(request);
        }

        return runInTransaction(request);
    }

    @Transactional
    public Appointment cancelAppointment(UUID appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        appointment.cancel(reason);
        resourceCapacityService.release(appointmentId);
        return appointmentRepository.save(appointment);
    }

    public Appointment bookAdministrativeAppointment(AdministrativeAppointmentBookingRequest request) {
        if (transactionTemplate == null) {
            return bookAdministrativeAppointmentTransactional(request);
        }

        Appointment appointment = transactionTemplate.execute(status -> bookAdministrativeAppointmentTransactional(request));
        if (appointment == null) {
            throw new IllegalStateException("Transaction returned null appointment");
        }
        return appointment;
    }

    private Appointment bookAdministrativeAppointmentTransactional(AdministrativeAppointmentBookingRequest request) {
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

        Appointment appointment = Appointment.scheduleStaffMeeting(
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

        Appointment saved = appointmentRepository.save(appointment);
        resourceCapacityService.allocate(saved);
        return saved;
    }

    private Appointment bookTherapyUnderLock(AppointmentBookingRequest request) {
        String lockKey = request.scheduleId() + "|" + request.sedeId() + "|" + request.date()
                + "|" + request.time() + "|" + request.type();
        ReentrantLock lock = THERAPY_SLOT_LOCKS.computeIfAbsent(lockKey, ignored -> new ReentrantLock());

        lock.lock();
        try {
            return runInTransaction(request);
        } finally {
            lock.unlock();
        }
    }

    private Appointment runInTransaction(AppointmentBookingRequest request) {
        if (transactionTemplate == null) {
            return bookAppointmentTransactional(request);
        }

        Appointment appointment = transactionTemplate.execute(status -> bookAppointmentTransactional(request));
        if (appointment == null) {
            throw new IllegalStateException("Transaction returned null appointment");
        }
        return appointment;
    }

    private Appointment bookAppointmentTransactional(AppointmentBookingRequest request) {
        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        int blockDuration = schedule.getSlotDurationMinutes();
        UUID consultorioId = schedule.getConsultorioId();

        HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                request.patientId(),
                request.doctorId(),
                request.secondaryDoctorId(),
                request.scheduleId(),
                request.sedeId(),
                request.date(),
                request.time(),
                request.type(),
                blockDuration
        );

        humanResourceAvailabilityService.assertBookingAllowed(context);

        if (isTherapy(request.type())) {
            humanResourceAvailabilityService.assertTherapyGroupAllowsNewPatient(
                    request.scheduleId(),
                    request.date(),
                    request.time(),
                    request.type()
            );
            Appointment saved = bookTherapyAppointment(request, blockDuration, consultorioId);
            resourceCapacityService.allocate(saved, consultorioId);
            return saved;
        }

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
                request.secondaryDoctorId(),
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

    private Appointment bookTherapyAppointment(AppointmentBookingRequest request, Integer duration, UUID consultorioId) {
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
                duration,
                null,
                consultorioId
        );

        AppointmentStatus initialStatus = activeGroupSize + 1 < HumanResourceAvailabilityService.THERAPY_GROUP_MIN
                ? AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO
                : AppointmentStatus.SCHEDULED;

        Appointment appointment = Appointment.scheduleNew(
                request.patientId(),
                request.doctorId(),
                request.secondaryDoctorId(),
                new AppointmentScheduleData(
                        request.scheduleId(),
                        request.sedeId(),
                        request.date(),
                        request.time(),
                        duration,
                        request.type(),
                        initialStatus,
                        request.reason()
                ),
                request.resolvedChannel(),
                request.n8nConversationId()
        );

        Appointment saved = appointmentRepository.save(appointment);

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
