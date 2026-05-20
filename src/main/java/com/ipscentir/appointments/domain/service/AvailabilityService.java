package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
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

    public List<AvailableSlotDetail> getNearestAvailableSlotsByServiceType(
            AppointmentServiceType serviceType,
            UUID facilityId,
            LocalDate fromDate,
            int limit
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }

        LocalDate startDate = fromDate == null ? LocalDate.now() : fromDate;
        List<AvailableSlotDetail> nearestSlots = new ArrayList<>();

        for (int offset = 0; offset < DEFAULT_SEARCH_DAYS && nearestSlots.size() < limit; offset++) {
            LocalDate currentDate = startDate.plusDays(offset);
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            List<Schedule> schedules = scheduleRepository.findByFacilityIdAndDayOfWeek(facilityId, dayOfWeek);
            for (Schedule schedule : schedules) {
                if (!matchesServiceType(schedule.getSpecialty(), serviceType)) {
                    continue;
                }

                List<LocalTime> possibleSlots = schedule.getAvailableSlots(currentDate);
                List<Appointment> existingAppointments = appointmentRepository.findByDoctorIdAndDate(schedule.getDoctorId(), currentDate);
                Map<LocalTime, Long> appointmentsPerSlot = existingAppointments.stream()
                        .collect(Collectors.groupingBy(Appointment::getAppointmentTime, Collectors.counting()));

                for (LocalTime time : possibleSlots) {
                    long occupiedSeats = appointmentsPerSlot.getOrDefault(time, 0L);
                    int availableSeats = Math.max(0, schedule.getMaxPatientsPerSlot() - (int) occupiedSeats);

                    if (availableSeats <= 0) {
                        continue;
                    }

                    nearestSlots.add(new AvailableSlotDetail(
                            schedule.getId(),
                            schedule.getDoctorId(),
                            schedule.getFacilityId(),
                            serviceType,
                            schedule.getSpecialty(),
                            currentDate,
                            time,
                            schedule.getSlotDurationMinutes(),
                            availableSeats
                    ));
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

    public List<AvailableSlot> getAvailableSlots(String doctorId, UUID facilityId, LocalDate date) {

        // 1. Obtener agenda del doctor para sede y día de la semana.
        Schedule schedule = scheduleRepository
            .findByDoctorIdAndFacilityIdAndDayOfWeek(doctorId, facilityId, date.getDayOfWeek())
            .orElseThrow(() -> new IllegalArgumentException(
                "No hay agenda configurada para este medico en la sede solicitada para " + date.getDayOfWeek()
            ));

        // 2. Obtener slots posibles según configuración (incluye filtrado de bloqueos internos).
        List<LocalTime> possibleSlots = schedule.getAvailableSlots(date);

        // 3. Obtener citas ya agendadas/confirmadas para ese día.
        List<Appointment> existingAppointments = appointmentRepository.findByDoctorIdAndDate(doctorId, date);

        // 4. Filtrar slots ya ocupados agrupándolos.
        Map<LocalTime, Long> appointmentsPerSlot = existingAppointments.stream()
            .collect(Collectors.groupingBy(
                Appointment::getAppointmentTime,
                Collectors.counting()
            ));

        // 5. Construir lista de slots disponibles que no excedan el cupo por slot.
        return possibleSlots.stream()
                .filter(time -> {
                    long appointmentsInSlot = appointmentsPerSlot.getOrDefault(time, 0L);
                    return appointmentsInSlot < schedule.getMaxPatientsPerSlot();
                })
                .map(time -> new AvailableSlot(
                        date,
                        time,
                        schedule.getSlotDurationMinutes()
                ))
                .toList();
    }

    public List<AvailableSlot> getAvailableSlots(String doctorId, LocalDate date) {
        
        // 1. Obtener agenda del doctor para ese día de la semana
        Schedule schedule = scheduleRepository
            .findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek())
            .orElseThrow(() -> new IllegalArgumentException(
                "No hay agenda configurada para este médico en " + date.getDayOfWeek()
            ));

        // 2. Obtener slots posibles según configuración (incluye filtrado de bloqueos internos)
        List<LocalTime> possibleSlots = schedule.getAvailableSlots(date);

        // 3. Obtener citas ya agendadas/confirmadas para ese día
        List<Appointment> existingAppointments = appointmentRepository.findByDoctorIdAndDate(doctorId, date);

        // 4. Filtrar slots ya ocupados agrupándolos
        Map<LocalTime, Long> appointmentsPerSlot = existingAppointments.stream()
            .collect(Collectors.groupingBy(
                Appointment::getAppointmentTime,
                Collectors.counting()
            ));

        // 5. Construir lista de slots disponibles que no excedan el cupo por slot
        return possibleSlots.stream()
            .filter(time -> {
                long appointmentsInSlot = appointmentsPerSlot.getOrDefault(time, 0L);
                return appointmentsInSlot < schedule.getMaxPatientsPerSlot();
            })
            .map(time -> new AvailableSlot(
                date,
                time,
                schedule.getSlotDurationMinutes()
            ))
            .toList();
    }

    public boolean isSlotAvailable(String doctorId, LocalDate date, LocalTime time) {
        // En caso de que el schedule repository falle (porque no haya schedule ese día), asumimos falso
        try {
            List<AvailableSlot> availableSlots = getAvailableSlots(doctorId, date);
            return availableSlots.stream()
                .anyMatch(slot -> slot.getTime().equals(time));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isSlotAvailable(String doctorId, UUID facilityId, LocalDate date, LocalTime time) {
        try {
            List<AvailableSlot> availableSlots = getAvailableSlots(doctorId, facilityId, date);
            return availableSlots.stream()
                    .anyMatch(slot -> slot.getTime().equals(time));
        } catch (IllegalArgumentException ex) {
            return false;
        }
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
