package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.AppointmentType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Datos necesarios para validar disponibilidad humana al agendar o reprogramar.
 */
public record HumanResourceBookingContext(
        UUID patientId,
        String primaryDoctorId,
        List<String> additionalDoctorIds,
        UUID scheduleId,
        Integer sedeId,
        LocalDate date,
        LocalTime time,
        AppointmentType appointmentType,
        int durationMinutes
) {
    public static HumanResourceBookingContext forBooking(
            UUID patientId,
            String primaryDoctorId,
            List<String> additionalDoctorIds,
            UUID scheduleId,
            Integer sedeId,
            LocalDate date,
            LocalTime time,
            AppointmentType appointmentType,
            int durationMinutes
    ) {
        return new HumanResourceBookingContext(
                patientId,
                primaryDoctorId,
                additionalDoctorIds != null ? additionalDoctorIds : Collections.emptyList(),
                scheduleId,
                sedeId,
                date,
                time,
                appointmentType,
                durationMinutes
        );
    }

}
