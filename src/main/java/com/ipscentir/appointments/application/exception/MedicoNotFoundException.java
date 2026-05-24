package com.ipscentir.appointments.application.exception;

public class MedicoNotFoundException extends RuntimeException {

    public MedicoNotFoundException(String medicoId) {
        super("Médico no encontrado: " + medicoId);
    }
}
