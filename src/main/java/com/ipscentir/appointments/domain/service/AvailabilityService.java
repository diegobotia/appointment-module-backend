package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlot;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlotDetail;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Locale.ROOT;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final int DEFAULT_SEARCH_DAYS = 90;

    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final ResourceCapacityService resourceCapacityService;
    private final FacilityOperatingHoursService facilityOperatingHoursService;

    public List<AvailableSlotDetail> getNearestAvailableSlotsByServiceType(
            AppointmentServiceType serviceType,
            Integer sedeId,
            LocalDate fromDate,
            int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }

        LocalDate startDate = fromDate == null ? LocalDate.now() : fromDate;
        List<AvailableSlotDetail> nearestSlots = new ArrayList<>();

        for (int offset = 0; offset < DEFAULT_SEARCH_DAYS && nearestSlots.size() < limit; offset++) {
            LocalDate currentDate = startDate.plusDays(offset);
            if (facilityOperatingHoursService.isHoliday(currentDate)) {
                continue;
            }
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            List<Schedule> schedules = scheduleRepository.findBySedeIdAndDayOfWeek(sedeId, dayOfWeek);
            for (Schedule schedule : schedules) {
                if (!matchesServiceType(schedule.getSpecialty(), serviceType)) {
                    continue;
                }

                List<LocalTime> possibleSlots = schedule.getAvailableSlots(currentDate);
                List<Appointment> existingAppointments = appointmentRepository
                        .findByDoctorIdAndDate(schedule.getDoctorId(), currentDate);
                Map<LocalTime, Long> appointmentsPerSlot = existingAppointments.stream()
                        .collect(Collectors.groupingBy(Appointment::getAppointmentTime, Collectors.counting()));

                for (LocalTime time : possibleSlots) {
                    if (currentDate.equals(LocalDate.now()) && time.isBefore(LocalTime.now().plusHours(1))) {
                        continue;
                    }

                    long occupiedSeats = appointmentsPerSlot.getOrDefault(time, 0L);
                    int availableSeats = Math.max(0, schedule.getMaxPatientsPerSlot() - (int) occupiedSeats);

                    if (availableSeats <= 0) {
                        continue;
                    }

                    boolean physicalCapacity = resourceCapacityService.hasPhysicalCapacityForService(
                            sedeId,
                            serviceType,
                            schedule.getId(),
                            currentDate,
                            time,
                            schedule.getSlotDurationMinutes());
                    if (!physicalCapacity) {
                        continue;
                    }

                    nearestSlots.add(new AvailableSlotDetail(
                            schedule.getId(),
                            schedule.getDoctorId(),
                            schedule.getSedeId(),
                            serviceType,
                            schedule.getSpecialty(),
                            currentDate,
                            time,
                            schedule.getSlotDurationMinutes(),
                            availableSeats,
                            true));
                }
            }
        }

        return nearestSlots.stream()
                .sorted(Comparator
                        .comparing(AvailableSlotDetail::appointmentDate)
                        .thenComparing(AvailableSlotDetail::appointmentTime)
                        .thenComparing(AvailableSlotDetail::doctorId))
                .limit(limit)
                .toList();
    }

    public List<AvailableSlot> getAvailableSlots(String doctorId, Integer sedeId, LocalDate date) {
        return getAvailableSlots(doctorId, sedeId, date, null);
    }

    public List<AvailableSlot> getAvailableSlots(String doctorId, Integer sedeId, LocalDate date,
            UUID excludeAppointmentId) {

        if (facilityOperatingHoursService.isHoliday(date)) {
            return List.of();
        }

        // 1. Obtener agenda del doctor para sede y día de la semana.
        Schedule schedule = scheduleRepository
                .findByDoctorIdAndSedeIdAndDayOfWeek(doctorId, sedeId, date.getDayOfWeek())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay agenda configurada para este medico en la sede solicitada para "
                                + date.getDayOfWeek()));

        // 2. Obtener slots posibles según configuración (incluye filtrado de bloqueos
        // internos).
        List<LocalTime> possibleSlots = schedule.getAvailableSlots(date);

        // 3. Obtener citas ya agendadas/confirmadas para ese día.
        List<Appointment> existingAppointments = appointmentRepository.findByDoctorIdAndDate(doctorId, date).stream()
                .filter(appointment -> excludeAppointmentId == null
                        || !excludeAppointmentId.equals(appointment.getId()))
                .toList();

        // 4. Filtrar slots ya ocupados agrupándolos.
        Map<LocalTime, Long> appointmentsPerSlot = existingAppointments.stream()
                .collect(Collectors.groupingBy(
                        Appointment::getAppointmentTime,
                        Collectors.counting()));

        // 5. Construir lista de slots disponibles que no excedan el cupo por slot.
        return possibleSlots.stream()
                .filter(time -> !(date.equals(LocalDate.now()) && time.isBefore(LocalTime.now().plusHours(1))))
                .filter(time -> {
                    long appointmentsInSlot = appointmentsPerSlot.getOrDefault(time, 0L);
                    return appointmentsInSlot < schedule.getMaxPatientsPerSlot();
                })
                .map(time -> new AvailableSlot(
                        date,
                        time,
                        schedule.getSlotDurationMinutes()))
                .toList();
    }

    public List<AvailableSlot> getAvailableSlots(String doctorId, LocalDate date) {
        return getAvailableSlots(doctorId, date, null);
    }

    public List<AvailableSlot> getAvailableSlots(String doctorId, LocalDate date, UUID excludeAppointmentId) {
        if (facilityOperatingHoursService.isHoliday(date)) {
            return List.of();
        }
        // 1. Obtener agenda del doctor para ese día de la semana
        Schedule schedule = scheduleRepository
                .findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay agenda configurada para este médico en " + date.getDayOfWeek()));

        // 2. Obtener slots posibles según configuración (incluye filtrado de bloqueos
        // internos)
        List<LocalTime> possibleSlots = schedule.getAvailableSlots(date);

        // 3. Obtener citas ya agendadas/confirmadas para ese día
        List<Appointment> existingAppointments = appointmentRepository.findByDoctorIdAndDate(doctorId, date).stream()
                .filter(appointment -> excludeAppointmentId == null
                        || !excludeAppointmentId.equals(appointment.getId()))
                .toList();

        // 4. Filtrar slots ya ocupados agrupándolos
        Map<LocalTime, Long> appointmentsPerSlot = existingAppointments.stream()
                .collect(Collectors.groupingBy(
                        Appointment::getAppointmentTime,
                        Collectors.counting()));

        // 5. Construir lista de slots disponibles que no excedan el cupo por slot
        return possibleSlots.stream()
                .filter(time -> !(date.equals(LocalDate.now()) && time.isBefore(LocalTime.now().plusHours(1))))
                .filter(time -> {
                    long appointmentsInSlot = appointmentsPerSlot.getOrDefault(time, 0L);
                    return appointmentsInSlot < schedule.getMaxPatientsPerSlot();
                })
                .map(time -> new AvailableSlot(
                        date,
                        time,
                        schedule.getSlotDurationMinutes()))
                .toList();
    }

    public boolean isSlotAvailable(String doctorId, LocalDate date, LocalTime time) {
        return isSlotAvailable(doctorId, date, time, null);
    }

    public boolean isSlotAvailable(String doctorId, LocalDate date, LocalTime time, UUID excludeAppointmentId) {
        // En caso de que el schedule repository falle (porque no haya schedule ese
        // día), asumimos falso
        try {
            List<AvailableSlot> availableSlots = getAvailableSlots(doctorId, date, excludeAppointmentId);
            return availableSlots.stream()
                    .anyMatch(slot -> slot.getTime().equals(time));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isSlotAvailable(String doctorId, Integer sedeId, LocalDate date, LocalTime time) {
        return isSlotAvailable(doctorId, sedeId, date, time, null, null, 30);
    }

    public boolean isSlotAvailable(String doctorId, Integer sedeId, LocalDate date, LocalTime time,
            UUID excludeAppointmentId) {
        return isSlotAvailable(doctorId, sedeId, date, time, null, null, 30, excludeAppointmentId);
    }

    /**
     * Disponibilidad del médico en agenda (cupo por slot), sin validar inventario
     * físico de sede.
     */
    public boolean isDoctorSlotAvailable(String doctorId, Integer sedeId, LocalDate date, LocalTime time) {
        return isDoctorSlotAvailable(doctorId, sedeId, date, time, null);
    }

    public boolean isDoctorSlotAvailable(String doctorId, Integer sedeId, LocalDate date, LocalTime time,
            UUID excludeAppointmentId) {
        try {
            List<AvailableSlot> availableSlots = getAvailableSlots(doctorId, sedeId, date, excludeAppointmentId);
            return availableSlots.stream().anyMatch(slot -> slot.getTime().equals(time));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isSlotAvailable(
            String doctorId,
            Integer sedeId,
            LocalDate date,
            LocalTime time,
            AppointmentType appointmentType,
            UUID scheduleId,
            int durationMinutes) {
        return isSlotAvailable(doctorId, sedeId, date, time, appointmentType, scheduleId, durationMinutes, null);
    }

    public boolean isSlotAvailable(
            String doctorId,
            Integer sedeId,
            LocalDate date,
            LocalTime time,
            AppointmentType appointmentType,
            UUID scheduleId,
            int durationMinutes,
            UUID excludeAppointmentId) {
        if (!isDoctorSlotAvailable(doctorId, sedeId, date, time, excludeAppointmentId)) {
            return false;
        }
        if (appointmentType == null) {
            return true;
        }
        try {
            UUID effectiveScheduleId = scheduleId != null ? scheduleId : resolveScheduleId(doctorId, sedeId, date);
            return resourceCapacityService.hasPhysicalCapacity(
                    sedeId,
                    appointmentType,
                    effectiveScheduleId,
                    date,
                    time,
                    durationMinutes,
                    null);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private UUID resolveScheduleId(String doctorId, Integer sedeId, LocalDate date) {
        return scheduleRepository
                .findByDoctorIdAndSedeIdAndDayOfWeek(doctorId, sedeId, date.getDayOfWeek())
                .map(Schedule::getId)
                .orElse(null);
    }

    /**
     * Slots disponibles con cupos restantes para un médico en sede y rango de
     * fechas (inclusive).
     */
    public List<AvailableSlotDetail> getAvailableSlotsInRange(
            String doctorId,
            Integer sedeId,
            LocalDate fromDate,
            LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must be on or after fromDate");
        }
        if (fromDate.plusDays(90).isBefore(toDate)) {
            throw new IllegalArgumentException("Date range cannot exceed 90 days");
        }

        List<AvailableSlotDetail> result = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            if (facilityOperatingHoursService.isHoliday(date)) {
                continue;
            }
            try {
                Schedule schedule = scheduleRepository
                        .findByDoctorIdAndSedeIdAndDayOfWeek(doctorId, sedeId, date.getDayOfWeek())
                        .orElse(null);
                if (schedule == null || !Boolean.TRUE.equals(schedule.getIsActive())) {
                    continue;
                }

                List<LocalTime> possibleSlots = schedule.getAvailableSlots(date);
                List<Appointment> existingAppointments = appointmentRepository.findByDoctorIdAndDate(doctorId, date);
                Map<LocalTime, Long> appointmentsPerSlot = existingAppointments.stream()
                        .collect(Collectors.groupingBy(Appointment::getAppointmentTime, Collectors.counting()));

                for (LocalTime time : possibleSlots) {
                    if (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now().plusHours(1))) {
                        continue;
                    }
                    long occupiedSeats = appointmentsPerSlot.getOrDefault(time, 0L);
                    int availableSeats = Math.max(0, schedule.getMaxPatientsPerSlot() - (int) occupiedSeats);
                    if (availableSeats <= 0) {
                        continue;
                    }
                    result.add(new AvailableSlotDetail(
                            schedule.getId(),
                            schedule.getDoctorId(),
                            schedule.getSedeId(),
                            null,
                            schedule.getSpecialty(),
                            date,
                            time,
                            schedule.getSlotDurationMinutes(),
                            availableSeats));
                }
            } catch (IllegalArgumentException ignored) {
                // Sin agenda ese día de la semana en la sede.
            }
        }
        return result;
    }

    private boolean matchesServiceType(String scheduleSpecialty, AppointmentServiceType serviceType) {
        if (scheduleSpecialty == null || scheduleSpecialty.isBlank()) {
            return false;
        }

        return serviceType.matches(scheduleSpecialty)
                || normalize(scheduleSpecialty).equals(normalize(serviceType.getDisplayName()))
                || normalize(scheduleSpecialty).equals(normalize(serviceType.name()));
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.trim().toLowerCase(ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
