package com.ipscentir.appointments.application.dto.integration.n8n;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record N8nPatientIdentifyRequest(
        @NotBlank
        @Schema(
                description = "Descripción del tipo de documento tal como la envía n8n (ej. Cédula de ciudadanía). "
                        + "También acepta código DIAN (13) o alias legacy (CC).",
                example = "Cédula de ciudadanía"
        )
        String codTipoIdentificacion,
        @NotBlank String numIdentificacion
) {
}
