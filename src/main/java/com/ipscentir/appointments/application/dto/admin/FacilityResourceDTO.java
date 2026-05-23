package com.ipscentir.appointments.application.dto.admin;

import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;

import java.util.UUID;

public record FacilityResourceDTO(
        UUID id,
        FacilityResourceType resourceType,
        String code,
        String displayName,
        int capacityUnits,
        boolean active
) {
}
