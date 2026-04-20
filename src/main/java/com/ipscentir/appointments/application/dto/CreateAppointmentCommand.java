package com.ipscentir.appointments.application.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateAppointmentCommand(
        @NotNull(message = "Patient ID is required")
        UUID patientId,
        
        @NotNull(message = "Doctor ID is required")
        UUID doctorId,

        @NotNull(message = "Facility ID is required")
        UUID facilityId,

        UUID secondaryDoctorId,
        
        @NotNull(message = "Schedule ID is required")
        UUID scheduleId,
        
        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date must be today or in the future")
        LocalDate appointmentDate,
        
        @NotNull(message = "Appointment time is required")
        LocalTime appointmentTime,
        
        @NotNull(message = "Appointment type is required (PRESENCIAL or TELEMEDICINA)")
        String appointmentType,
        
        String reason
) {}
