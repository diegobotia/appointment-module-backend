package com.ipscentir.appointments.application.dto;

import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentDTO(
        UUID id,
        UUID patientId,
        String doctorId,
        Integer sedeId,
        String secondaryDoctorId,
        UUID scheduleId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        Integer durationMinutes,
        AppointmentType appointmentType,
        AppointmentStatus status,
        BookingChannel bookingChannel,
        String n8nConversationId,
        String reason,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
