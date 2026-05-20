package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class AppointmentBookingService {

    private static final int THERAPY_GROUP_MIN = 4;
    private static final int THERAPY_GROUP_MAX = 6;
    private static final ConcurrentHashMap<String, ReentrantLock> THERAPY_SLOT_LOCKS = new ConcurrentHashMap<>();

    private final AvailabilityService availabilityService;
    private final AppointmentRepository appointmentRepository;
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
        // Dispatch DomainEvents upon saving:
        return appointmentRepository.save(appointment);
    }

    private Appointment bookTherapyUnderLock(AppointmentBookingRequest request) {
        String lockKey = request.scheduleId() + "|" + request.facilityId() + "|" + request.date() + "|" + request.time() + "|" + request.type();
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
        UUID patientId = request.patientId();
        String doctorId = request.doctorId();
        String secondaryDoctorId = request.secondaryDoctorId();
        UUID scheduleId = request.scheduleId();
        UUID facilityId = request.facilityId();
        LocalDate date = request.date();
        LocalTime time = request.time();
        AppointmentType type = request.type();
        String reason = request.reason();

        validateScheduleOwnership(scheduleId, doctorId, facilityId);
        validateDoctorAvailability(doctorId, facilityId, date, time);
        validateJuntaMedica(type, doctorId, facilityId, secondaryDoctorId, date, time);
        validatePatientNoDuplicate(patientId, date);

        // Regla 3: Crear la Cita Médica
        // El Default es de 30 mins, podemos adaptarlo después consultando schedule duration si existe.
        Integer blockDuration = 30;

        if (isTherapy(type)) {
            return bookTherapyAppointment(request, blockDuration);
        }

        Appointment appointment = Appointment.scheduleNew(
                patientId,
            doctorId,
            secondaryDoctorId,
            new AppointmentScheduleData(
                scheduleId,
                facilityId,
                date,
                time,
                blockDuration,
                type,
                AppointmentStatus.SCHEDULED,
                reason
            )
        );
        return appointmentRepository.save(appointment);
    }

    private void validateScheduleOwnership(UUID scheduleId, String doctorId, UUID facilityId) {
        var schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        if (!schedule.getDoctorId().equals(doctorId)) {
            throw new IllegalStateException("The selected schedule does not belong to the requested doctor.");
        }

        if (!schedule.getFacilityId().equals(facilityId)) {
            throw new IllegalStateException("The selected schedule does not belong to the requested facility.");
        }
    }

    private void validateDoctorAvailability(String doctorId, UUID facilityId, LocalDate date, LocalTime time) {
        // Regla 1: Validar si la franja está disponible en la agenda del doctor
        // integrando comprobación de horarios bloqueados y citas previas cruzadas.
        if (!availabilityService.isSlotAvailable(doctorId, facilityId, date, time)) {
            throw new IllegalStateException("The requested slot is not available for this doctor.");
        }
    }

    private void validateJuntaMedica(
            AppointmentType type,
            String doctorId,
            UUID facilityId,
            String secondaryDoctorId,
            LocalDate date,
            LocalTime time
    ) {
        if (type != AppointmentType.JUNTA_MEDICA) {
            return;
        }

        if (secondaryDoctorId == null) {
            throw new IllegalStateException("Junta medica requires exactly 2 specialists");
        }
        if (secondaryDoctorId.equals(doctorId)) {
            throw new IllegalStateException("Junta medica requires 2 different specialists");
        }
        if (!availabilityService.isSlotAvailable(secondaryDoctorId, facilityId, date, time)) {
            throw new IllegalStateException("The requested slot is not available for the second specialist.");
        }
    }

    private void validatePatientNoDuplicate(UUID patientId, LocalDate date) {
        // Regla 2: Un paciente no puede tener dos citas agendadas el mismo día (protección simple anti-fraude)
        if (appointmentRepository.existsByPatientIdAndDate(patientId, date)) {
            throw new IllegalStateException("The patient already has an active appointment for this date.");
        }
    }

    private boolean isTherapy(AppointmentType type) {
        return type == AppointmentType.TERAPIA_FISICA || type == AppointmentType.TERAPIA_OCUPACIONAL;
    }

    private Appointment bookTherapyAppointment(AppointmentBookingRequest request, Integer duration) {
        UUID patientId = request.patientId();
        String doctorId = request.doctorId();
        String secondaryDoctorId = request.secondaryDoctorId();
        UUID scheduleId = request.scheduleId();
        UUID facilityId = request.facilityId();
        LocalDate date = request.date();
        LocalTime time = request.time();
        AppointmentType type = request.type();
        String reason = request.reason();

        List<Appointment> slotAppointments = appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(
            scheduleId,
            date,
            time,
            type
        );
        long activeGroupSize = slotAppointments.stream()
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
            .count();

        if (activeGroupSize >= THERAPY_GROUP_MAX) {
            throw new IllegalStateException("Therapy slot has reached maximum capacity (6)");
        }

        AppointmentStatus initialStatus = activeGroupSize + 1 < THERAPY_GROUP_MIN
            ? AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO
            : AppointmentStatus.SCHEDULED;

        Appointment appointment = Appointment.scheduleNew(
            patientId,
            doctorId,
            secondaryDoctorId,
            new AppointmentScheduleData(
                    scheduleId,
                    facilityId,
                    date,
                    time,
                    duration,
                    type,
                    initialStatus,
                    reason
            )
        );

        Appointment saved = appointmentRepository.save(appointment);

        List<Appointment> refreshed = appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(
            scheduleId,
            date,
            time,
            type
        );

        long refreshedActiveGroupSize = refreshed.stream()
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
            .count();

        if (refreshedActiveGroupSize >= THERAPY_GROUP_MIN) {
            refreshed.forEach(Appointment::transitionGroupPendingToScheduled);
            appointmentRepository.saveAll(refreshed);
        }

        return saved;
    }
}
