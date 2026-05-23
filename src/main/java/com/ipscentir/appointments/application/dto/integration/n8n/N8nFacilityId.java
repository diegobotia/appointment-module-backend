package com.ipscentir.appointments.application.dto.integration.n8n;

public enum N8nFacilityId {
    CONQUISTADORES(1),
    BELEN(2);

    private final int sedeId;

    N8nFacilityId(int sedeId) {
        this.sedeId = sedeId;
    }

    public int sedeId() {
        return sedeId;
    }

    public String legacyAliasCode() {
        return switch (this) {
            case CONQUISTADORES -> "SEDE_CONQUISTADORES";
            case BELEN -> "SEDE_BELEN";
        };
    }
}
