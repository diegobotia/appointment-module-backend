package com.ipscentir.appointments.application.dto.form;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientRegistrationResponse(
        UUID patientId,
        String codTipoIdentificacion,
        String numIdentificacion,
        String nombres,
        String apellidos,
        String email,
        String message,
        LocalDateTime createdAt
) {
}
