package com.ipscentir.appointments.application.exception;

public class PatientAlreadyExistsException extends IllegalArgumentException {

    public PatientAlreadyExistsException(String codTipoIdentificacion, String numIdentificacion) {
        super("Ya existe un paciente con documento " + codTipoIdentificacion + " " + numIdentificacion);
    }
}
