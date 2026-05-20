package com.ipscentir.appointments.application.dto;

import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentDTO(
        UUID id,
        UUID patientId,
        String doctorId,
        UUID facilityId,
        String secondaryDoctorId,
        UUID scheduleId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        Integer durationMinutes,
        AppointmentType appointmentType,
        AppointmentStatus status,
        String reason,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
