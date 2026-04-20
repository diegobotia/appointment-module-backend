package com.ipscentir.appointments.application.dto.integration.n8n;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record N8nPatientAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID doctorId,
        @NotNull UUID facilityId,
        UUID secondaryDoctorId,
        @NotNull UUID scheduleId,
        @NotNull @FutureOrPresent LocalDate appointmentDate,
        @NotNull LocalTime appointmentTime,
        String reason
) {}