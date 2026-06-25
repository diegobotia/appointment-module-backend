package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.application.service.MedicoLookupService;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HumanResourceAvailabilityService {

    public static final int THERAPY_GROUP_MIN = 4;
    public static final int THERAPY_GROUP_MAX = 6;

    private final AvailabilityService availabilityService;
    private final FacilityOperatingHoursService facilityOperatingHoursService;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicoLookupService medicoLookupService;

    public void assertBookingAllowed(HumanResourceBookingContext context) {
        facilityOperatingHoursService.assertDateNotHoliday(context.date());
        assertBookingAllowed(context, null);
    }

    public void assertBookingAllowed(HumanResourceBookingContext context, UUID excludeAppointmentId) {
        if (context.appointmentType() == AppointmentType.BLOQUEO) {
            assertBloqueoAllowed(context, excludeAppointmentId);
            return;
        }
        Schedule schedule = loadSchedule(context.scheduleId());
        assertScheduleOwnership(schedule, context.primaryDoctorId(), context.sedeId());
        assertSlotWithinSedeHours(context);
        ServiceResourceMatrix.assertScheduleAlignsWithAppointmentType(schedule, context.appointmentType());
        assertPrimaryDoctorAvailable(context, excludeAppointmentId);
        assertJuntaMedicaRules(context, excludeAppointmentId);
        assertPatientNoDuplicate(context.patientId(), context.date(), excludeAppointmentId);
    }

    private void assertBloqueoAllowed(HumanResourceBookingContext context, UUID excludeAppointmentId) {
        List<String> specialties = medicoLookupService.findActiveSpecialties(context.primaryDoctorId());
        if (specialties.stream().noneMatch(s -> s.equalsIgnoreCase("DOLOR"))) {
            throw new IllegalStateException(
                    "El bloqueo solo puede asignarse a un médico con especialidad en Dolor.");
        }
        if (!availabilityService.isDoctorSlotAvailable(
                context.primaryDoctorId(),
                context.sedeId(),
                context.date(),
                context.time(),
                excludeAppointmentId)) {
            throw new IllegalStateException("The requested slot is not available for the pain management doctor.");
        }
        assertNoRangeOverlap(context.primaryDoctorId(), context, excludeAppointmentId);
        assertPatientNoDuplicate(context.patientId(), context.date(), excludeAppointmentId);
    }

    public void assertRescheduleAllowed(HumanResourceBookingContext context, UUID appointmentId) {
        assertBookingAllowed(context, appointmentId);
    }

    public void assertTherapyGroupAllowsNewPatient(
            UUID scheduleId,
            LocalDate date,
            LocalTime time,
            AppointmentType type) {
        if (!isTherapy(type)) {
            return;
        }

        List<Appointment> slotAppointments = appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(
                scheduleId,
                date,
                time,
                type);
        long activeGroupSize = slotAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .count();

        if (activeGroupSize >= THERAPY_GROUP_MAX) {
            throw new IllegalStateException(
                    "Therapy slot has reached maximum capacity (" + THERAPY_GROUP_MAX + ")");
        }
    }

    /**
     * Override administrativo: salta horario de sede, festivos, agenda del doctor y capacidad.
     * Solo valida que el doctor no tenga otra cita en la misma franja y que el paciente
     * no tenga duplicado en la misma fecha.
     */
    public void assertAdminOverrideAllowed(HumanResourceBookingContext context, UUID excludeAppointmentId) {
        assertNoRangeOverlap(context.primaryDoctorId(), context, excludeAppointmentId);
        if (context.additionalDoctorIds() != null) {
            for (String doctorId : context.additionalDoctorIds()) {
                assertNoRangeOverlap(doctorId, context, excludeAppointmentId);
            }
        }
        assertPatientNoDuplicate(context.patientId(), context.date(), excludeAppointmentId);
    }

    /**
     * Valida disponibilidad para citas administrativas (reuniones staff sin paciente).
     */
    public void assertAdministrativeBookingAllowed(
            List<String> participantDoctorIds,
            Integer sedeId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes
    ) {
        facilityOperatingHoursService.assertDateNotHoliday(date);
        assertStaffMeetingParticipantsAvailable(participantDoctorIds, sedeId, date, startTime, durationMinutes);
    }

    /**
     * Reservado para Fase E (junta staff). Valida que todos los participantes estén libres en la franja.
     */
    public void assertStaffMeetingParticipantsAvailable(
            List<String> participantDoctorIds,
            Integer sedeId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes) {
        if (participantDoctorIds == null || participantDoctorIds.isEmpty()) {
            throw new IllegalStateException("Junta staff requiere al menos un participante");
        }
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        for (String doctorId : participantDoctorIds) {
            if (!availabilityService.isDoctorSlotAvailable(doctorId, sedeId, date, startTime)) {
                throw new IllegalStateException(
                        "El participante " + doctorId + " no está disponible en la franja solicitada");
            }
            facilityOperatingHoursService.assertSlotWithinSedeHours(
                    sedeId,
                    date.getDayOfWeek(),
                    startTime,
                    endTime);
            assertDoctorNoRangeOverlap(doctorId, sedeId, date, startTime, endTime);
        }
    }

    private void assertScheduleOwnership(Schedule schedule, String doctorId, Integer sedeId) {
        if (!schedule.getDoctorId().equals(doctorId)) {
            throw new IllegalStateException("The selected schedule does not belong to the requested doctor.");
        }
        if (!schedule.getSedeId().equals(sedeId)) {
            throw new IllegalStateException("The selected schedule does not belong to the requested facility.");
        }
    }

    private void assertSlotWithinSedeHours(HumanResourceBookingContext context) {
        LocalTime endTime = context.time().plusMinutes(context.durationMinutes());
        facilityOperatingHoursService.assertSlotWithinSedeHours(
                context.sedeId(),
                context.date().getDayOfWeek(),
                context.time(),
                endTime);
    }

    private void assertPrimaryDoctorAvailable(HumanResourceBookingContext context, UUID excludeAppointmentId) {
        if (!availabilityService.isDoctorSlotAvailable(
                context.primaryDoctorId(),
                context.sedeId(),
                context.date(),
                context.time(),
                excludeAppointmentId)) {
            throw new IllegalStateException("The requested slot is not available for this doctor.");
        }
        if (!isTherapy(context.appointmentType())) {
            assertNoRangeOverlap(context.primaryDoctorId(), context, excludeAppointmentId);
        }
    }

    private void assertJuntaMedicaRules(HumanResourceBookingContext context, UUID excludeAppointmentId) {
        if (context.appointmentType() != AppointmentType.JUNTA_MEDICA) {
            return;
        }

        List<String> additional = context.additionalDoctorIds();
        if (additional == null || additional.isEmpty() || additional.size() > 3) {
            throw new IllegalStateException("Junta medica requires between 2 and 4 specialists");
        }

        for (String doctorId : additional) {
            if (doctorId.equals(context.primaryDoctorId())) {
                throw new IllegalStateException("Junta medica requires different specialists");
            }
            if (context.additionalDoctorIds().stream().filter(d -> d.equals(doctorId)).count() > 1) {
                throw new IllegalStateException("Duplicate specialist in junta medica");
            }
            if (!availabilityService.isDoctorSlotAvailable(
                    doctorId,
                    context.sedeId(),
                    context.date(),
                    context.time(),
                    excludeAppointmentId)) {
                throw new IllegalStateException("The requested slot is not available for specialist: " + doctorId);
            }
            if (!isTherapy(context.appointmentType())) {
                assertNoRangeOverlap(doctorId, context, excludeAppointmentId);
            }
        }
    }

    private void assertNoRangeOverlap(String doctorId, HumanResourceBookingContext context, UUID excludeAppointmentId) {
        LocalTime endTime = context.time().plusMinutes(context.durationMinutes());
        List<Appointment> existing = appointmentRepository.findByDoctorIdAndDate(doctorId, context.date());
        for (Appointment a : existing) {
            if (excludeAppointmentId != null && excludeAppointmentId.equals(a.getId())) continue;
            if (a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.NO_SHOW) continue;
            LocalTime existingEnd = a.getAppointmentTime().plusMinutes(a.getDurationMinutes());
            if (context.time().isBefore(existingEnd) && a.getAppointmentTime().isBefore(endTime)) {
                throw new IllegalStateException(
                        "El doctor " + doctorId + " ya tiene una cita en la franja "
                        + a.getAppointmentTime() + "-" + existingEnd);
            }
        }
    }

    private void assertPatientNoDuplicate(UUID patientId, LocalDate date, UUID excludeAppointmentId) {
        if (patientId == null) {
            return;
        }
        if (excludeAppointmentId != null) {
            if (appointmentRepository.existsByPatientIdAndDateExcluding(patientId, date, excludeAppointmentId)) {
                throw new IllegalStateException("The patient already has an active appointment for this date.");
            }
            return;
        }
        if (appointmentRepository.existsByPatientIdAndDate(patientId, date)) {
            throw new IllegalStateException("The patient already has an active appointment for this date.");
        }
    }

    private void assertDoctorNoRangeOverlap(String doctorId, Integer sedeId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Appointment> existing = appointmentRepository.findByDoctorIdAndDate(doctorId, date);
        for (Appointment a : existing) {
            if (a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.NO_SHOW) continue;
            LocalTime existingEnd = a.getAppointmentTime().plusMinutes(a.getDurationMinutes());
            if (startTime.isBefore(existingEnd) && a.getAppointmentTime().isBefore(endTime)) {
                throw new IllegalStateException(
                        "El participante " + doctorId + " ya tiene una cita en la franja "
                        + a.getAppointmentTime() + "-" + existingEnd);
            }
        }
    }

    private Schedule loadSchedule(UUID scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
    }

    private boolean isTherapy(AppointmentType type) {
        return type == AppointmentType.TERAPIA_FISICA || type == AppointmentType.TERAPIA_OCUPACIONAL;
    }
}
