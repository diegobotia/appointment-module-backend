package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlot;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;

    public List<AvailableSlot> getAvailableSlots(UUID doctorId, LocalDate date) {
        
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
            .collect(Collectors.toList());
    }

    public boolean isSlotAvailable(UUID doctorId, LocalDate date, LocalTime time) {
        // En caso de que el schedule repository falle (porque no haya schedule ese día), asumimos falso
        try {
            List<AvailableSlot> availableSlots = getAvailableSlots(doctorId, date);
            return availableSlots.stream()
                .anyMatch(slot -> slot.getTime().equals(time));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
