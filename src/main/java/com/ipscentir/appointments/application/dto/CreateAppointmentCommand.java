package com.ipscentir.appointments.application.dto;

import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateAppointmentCommand(
        @NotNull(message = "Patient ID is required")
        UUID patientId,
        
        @NotNull(message = "Doctor ID is required")
        String doctorId,

        @NotNull(message = "Facility ID is required")
        Integer sedeId,

        String secondaryDoctorId,
        
        @NotNull(message = "Schedule ID is required")
        UUID scheduleId,
        
        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date must be today or in the future")
        LocalDate appointmentDate,
        
        @NotNull(message = "Appointment time is required")
        LocalTime appointmentTime,
        
        @NotNull(message = "Appointment type is required")
        String appointmentType,
        
        String reason,
        BookingChannel bookingChannel,
        String n8nConversationId
) {
    public BookingChannel resolvedChannel() {
        return bookingChannel != null ? bookingChannel : BookingChannel.STAFF;
    }
}
