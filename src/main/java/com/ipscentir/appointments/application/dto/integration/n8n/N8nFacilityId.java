package com.ipscentir.appointments.application.dto.integration.n8n;

public enum N8nFacilityId {
    BELEN("SEDE_NORTE"),
    CONQUISTADORES("SEDE_PRINCIPAL");

    private final String persistenceCode;

    N8nFacilityId(String persistenceCode) {
        this.persistenceCode = persistenceCode;
    }

    public String persistenceCode() {
        return persistenceCode;
    }
}
