package com.ipscentir.appointments.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Creación de cita por override administrativo (salta horario, agenda y capacidad)")
public record CreateAdminOverrideAppointmentCommand(

        @NotNull(message = "Patient ID is required")
        UUID patientId,

        @NotNull(message = "Medico ID is required")
        @JsonAlias("doctorId")
        String medicoId,

        List<String> additionalMedicoIds,

        @NotNull(message = "Facility ID is required")
        Integer sedeId,

        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date must be today or in the future")
        LocalDate appointmentDate,

        @NotNull(message = "Appointment time is required")
        LocalTime appointmentTime,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        int durationMinutes,

        @NotBlank(message = "Appointment type is required")
        @Schema(description = "Tipo de cita (PRESENCIAL, JUNTA_MEDICA, etc.)")
        String appointmentType,

        @Schema(description = "Motivo de la cita")
        String reason
) {
}
