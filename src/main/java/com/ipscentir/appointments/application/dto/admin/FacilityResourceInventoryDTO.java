package com.ipscentir.appointments.application.dto.admin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FacilityResourceInventoryDTO(
        Integer sedeId,
        String sedeNombre,
        int totalActiveResources,
        Map<String, Integer> countByResourceType,
        List<FacilityResourceDTO> resources
) {
}
