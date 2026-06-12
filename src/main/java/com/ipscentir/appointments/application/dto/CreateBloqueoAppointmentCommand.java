package com.ipscentir.appointments.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Creación de cita tipo BLOQUEO (médico del dolor + paciente, sin recurso físico)")
public record CreateBloqueoAppointmentCommand(
        @NotNull(message = "Patient ID is required")
        UUID patientId,

        @NotNull(message = "Medico ID is required")
        @JsonAlias("doctorId")
        String medicoId,

        @NotNull(message = "Facility ID is required")
        Integer sedeId,

        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date must be today or in the future")
        LocalDate appointmentDate,

        @NotNull(message = "Appointment time is required")
        LocalTime appointmentTime,

        @Schema(description = "Motivo del bloqueo", example = "Bloqueo por dolor")
        String reason
) {
}
