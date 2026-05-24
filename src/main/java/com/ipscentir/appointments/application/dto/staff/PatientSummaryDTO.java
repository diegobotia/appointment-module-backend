package com.ipscentir.appointments.application.dto.staff;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resumen de paciente para mostrador y call center")
public record PatientSummaryDTO(
        UUID id,
        String nombres,
        String apellidos,
        @Schema(description = "nombres + apellidos", example = "María López")
        String fullName,
        @Schema(description = "Código DIAN", example = "13")
        String codTipoIdentificacion,
        @Schema(description = "Descripción del tipo de documento", example = "Cédula de ciudadanía")
        String tipoIdentificacionDescripcion,
        @Schema(example = "1234567890")
        String numIdentificacion
) {
}
