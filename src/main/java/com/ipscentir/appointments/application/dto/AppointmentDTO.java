package com.ipscentir.appointments.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Cita con nombres resueltos para tablas y calendario del panel interno")
public record AppointmentDTO(
        @Schema(description = "Identificador de la cita", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,
        @Schema(description = "Paciente asociado; null en citas administrativas (STAFF)", nullable = true)
        UUID patientId,
        @Schema(description = "ID del médico principal (hc.medicos.id)")
        @JsonAlias("doctorId")
        String medicoId,
        @Schema(description = "Sede donde se atiende la cita", example = "1")
        Integer sedeId,
        @Schema(description = "Médicos adicionales (junta médica o reunión staff)", nullable = true)
        @JsonAlias({"secondaryDoctorId", "secondaryMedicoId"})
        List<String> additionalMedicoIds,
        @Schema(description = "Plantilla de agenda usada al reservar", nullable = true)
        UUID scheduleId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        @Schema(example = "30")
        Integer durationMinutes,
        AppointmentType appointmentType,
        AppointmentStatus status,
        BookingChannel bookingChannel,
        @Schema(description = "Conversación n8n cuando bookingChannel=N8N", nullable = true)
        String n8nConversationId,
        String reason,
        @Schema(nullable = true)
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @Schema(
                description = "Nombre completo del médico principal; resuelto en servidor",
                example = "Ana Martínez",
                nullable = true
        )
        @JsonAlias("doctorDisplayName")
        String medicoDisplayName,
        @Schema(
                description = "Nombre completo del paciente; null si patientId es null",
                example = "Juan Pérez",
                nullable = true
        )
        String patientDisplayName,
        @Schema(
                description = "true cuando appointmentType=STAFF (reunión/bloqueo interno sin paciente)",
                example = "false"
        )
        boolean administrative
) {}
