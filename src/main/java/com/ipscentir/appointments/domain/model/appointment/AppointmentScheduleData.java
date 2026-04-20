package com.ipscentir.appointments.domain.model.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentScheduleData(
        UUID scheduleId,
        UUID facilityId,
        LocalDate date,
        LocalTime time,
        Integer duration,
        AppointmentType type,
        AppointmentStatus status,
        String reason
) {}