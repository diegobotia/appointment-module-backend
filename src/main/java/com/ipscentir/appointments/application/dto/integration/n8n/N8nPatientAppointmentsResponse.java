package com.ipscentir.appointments.application.dto.integration.n8n;

import java.util.List;
import java.util.UUID;

public record N8nPatientAppointmentsResponse(
        UUID patientId,
        String codTipoIdentificacion,
        String numIdentificacion,
        int total,
        List<N8nPatientAppointmentSummaryDTO> appointments,
        String summary
) {
}
