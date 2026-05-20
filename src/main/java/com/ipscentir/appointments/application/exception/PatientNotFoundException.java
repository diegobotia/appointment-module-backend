package com.ipscentir.appointments.application.exception;

import java.util.UUID;

public class PatientNotFoundException extends IllegalArgumentException {

    public PatientNotFoundException(String codTipoIdentificacion, String numIdentificacion) {
        super("Paciente no encontrado: " + codTipoIdentificacion + " " + numIdentificacion);
    }

    public PatientNotFoundException(UUID patientId) {
        super("Paciente no encontrado: " + patientId);
    }
}
