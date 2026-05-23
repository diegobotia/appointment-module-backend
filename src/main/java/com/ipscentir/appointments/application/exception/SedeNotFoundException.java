package com.ipscentir.appointments.application.exception;

public class SedeNotFoundException extends RuntimeException {

    public SedeNotFoundException(String codeOrAlias) {
        super("Sede no encontrada: " + codeOrAlias);
    }

    public SedeNotFoundException(Integer sedeId) {
        super("Sede no encontrada: " + sedeId);
    }
}
