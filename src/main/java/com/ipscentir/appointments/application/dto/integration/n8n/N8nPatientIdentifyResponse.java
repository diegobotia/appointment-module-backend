package com.ipscentir.appointments.application.dto.integration.n8n;

import java.util.UUID;

public record N8nPatientIdentifyResponse(
        boolean found,
        UUID patientId,
        String codTipoIdentificacion,
        String numIdentificacion,
        String nombres,
        String apellidos,
        String formUrl,
        String summary
) {
}
