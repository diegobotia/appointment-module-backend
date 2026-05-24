package com.ipscentir.appointments.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Alta de reunión o bloqueo entre personal interno (sin paciente; tipo STAFF)")
public record CreateAdministrativeAppointmentCommand(
        @Schema(
                description = "IDs de médicos participantes (hc.medicos.id). El primero es medicoId; el segundo, si existe, secondaryMedicoId",
                example = "[\"medico-uuid-1\", \"medico-uuid-2\"]"
        )
        @NotEmpty(message = "At least one participant is required")
        @JsonAlias("participantDoctorIds")
        List<@NotBlank(message = "Participant medico ID cannot be blank") String> participantMedicoIds,

        @NotNull(message = "Facility ID is required")
        Integer sedeId,

        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date must be today or in the future")
        LocalDate appointmentDate,

        @NotNull(message = "Appointment time is required")
        LocalTime appointmentTime,

        @Schema(description = "Duración en minutos; por defecto 30 si se omite", example = "60")
        @Min(value = 15, message = "Duration must be at least 15 minutes")
        @Max(value = 480, message = "Duration cannot exceed 480 minutes")
        Integer durationMinutes,

        @Schema(description = "Motivo visible en calendario", example = "Junta operativa")
        String reason
) {
    public static final int DEFAULT_DURATION_MINUTES = 30;

    public int resolvedDurationMinutes() {
        return durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MINUTES;
    }
}
