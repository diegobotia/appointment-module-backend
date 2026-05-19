package com.ipscentir.appointments.domain.model.pqrs;

public enum PqrsType {
    PETICION("Petición"),
    QUEJA("Queja"),
    RECLAMO("Reclamo"),
    SUGERENCIA("Sugerencia");

    private final String displayName;

    PqrsType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
