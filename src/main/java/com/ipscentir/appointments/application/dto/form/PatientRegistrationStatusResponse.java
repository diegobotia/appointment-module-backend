package com.ipscentir.appointments.application.dto.form;

import java.util.UUID;

public record PatientRegistrationStatusResponse(
        boolean registered,
        UUID patientId,
        String codTipoIdentificacion,
        String numIdentificacion,
        String nombres,
        String apellidos
) {
    public static PatientRegistrationStatusResponse notRegistered(String codTipoIdentificacion, String numIdentificacion) {
        return new PatientRegistrationStatusResponse(false, null, codTipoIdentificacion, numIdentificacion, null, null);
    }
}
