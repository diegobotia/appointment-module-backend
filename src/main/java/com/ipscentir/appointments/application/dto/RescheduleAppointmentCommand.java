package com.ipscentir.appointments.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RescheduleAppointmentCommand(
        @NotNull @FutureOrPresent LocalDate appointmentDate,
        @NotNull LocalTime appointmentTime,
        @NotNull UUID scheduleId,
        @NotNull @JsonAlias("doctorId") String medicoId,
        @NotNull Integer sedeId,
        String reason
) {
}
