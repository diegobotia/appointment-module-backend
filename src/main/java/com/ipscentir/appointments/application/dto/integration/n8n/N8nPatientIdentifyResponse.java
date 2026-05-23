package com.ipscentir.appointments.application.dto.integration.n8n;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record N8nPatientIdentifyResponse(
        boolean found,
        UUID patientId,
        @Schema(
                description = "Descripción oficial del tipo de documento (catálogo DIAN)",
                example = "Cédula de ciudadanía"
        )
        String tipoIdentificacion,
        String numIdentificacion,
        String nombres,
        String apellidos,
        String formUrl,
        String summary
) {
}
