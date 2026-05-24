package com.ipscentir.appointments.application.dto.medico;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Médico del directorio hc.medicos (solo lectura en este módulo)")
public record MedicoSummaryDTO(
        @Schema(description = "ID en hc.medicos.id")
        String id,
        String firstName,
        String lastName,
        @Schema(example = "Ana Martínez")
        String fullName,
        @Schema(description = "Número de documento")
        String numDoc,
        @Schema(description = "Registro médico (hc.medicos.registro)", example = "MED-003")
        String registro,
        String specialty,
        boolean active
) {
}
