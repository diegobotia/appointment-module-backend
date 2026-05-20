package com.ipscentir.appointments.application.dto.integration.n8n;

import jakarta.validation.constraints.NotBlank;

public record N8nPatientIdentifyRequest(
        @NotBlank String codTipoIdentificacion,
        @NotBlank String numIdentificacion
) {
}
