package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.admin.FacilityOperatingHourDTO;
import com.ipscentir.appointments.application.dto.admin.FacilityResourceDTO;
import com.ipscentir.appointments.application.dto.admin.FacilityResourceInventoryDTO;
import com.ipscentir.appointments.application.dto.admin.SedeSummaryDTO;
import com.ipscentir.appointments.domain.model.facility.FacilityResource;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.repository.FacilityOperatingHoursRepository;
import com.ipscentir.appointments.domain.repository.FacilityResourceRepository;
import com.ipscentir.appointments.domain.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminSedeService {

    private final SedeRepository sedeRepository;
    private final FacilityOperatingHoursRepository facilityOperatingHoursRepository;
    private final FacilityResourceRepository facilityResourceRepository;
    private final SedeLookupService sedeLookupService;

    @Transactional(readOnly = true)
    public List<SedeSummaryDTO> listSedes() {
        return sedeRepository.findAllOrderById().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SedeSummaryDTO getSede(Integer sedeId) {
        return toSummary(sedeLookupService.requireById(sedeId));
    }

    @Transactional(readOnly = true)
    public List<FacilityOperatingHourDTO> getOperatingHours(Integer sedeId) {
        sedeLookupService.requireById(sedeId);
        return facilityOperatingHoursRepository.findBySedeIdOrderByDayOfWeek(sedeId).stream()
                .map(this::toOperatingHourDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacilityResourceInventoryDTO getResourceInventory(Integer sedeId) {
        Sede sede = sedeLookupService.requireById(sedeId);
        List<FacilityResourceDTO> resources = facilityResourceRepository
                .findActiveBySedeIdOrderByTypeAndCode(sedeId).stream()
                .map(this::toResourceDto)
                .toList();

        Map<String, Integer> countByType = new LinkedHashMap<>();
        for (FacilityResourceType type : FacilityResourceType.values()) {
            countByType.put(type.name(), 0);
        }
        for (FacilityResourceDTO resource : resources) {
            countByType.merge(resource.resourceType().name(), 1, Integer::sum);
        }

        return new FacilityResourceInventoryDTO(
                sede.getId(),
                sede.getNombre(),
                resources.size(),
                countByType,
                resources
        );
    }

    private SedeSummaryDTO toSummary(Sede sede) {
        return new SedeSummaryDTO(
                sede.getId(),
                sede.getNombre(),
                sede.getDireccion(),
                sede.getMatriculaMercantil()
        );
    }

    private FacilityOperatingHourDTO toOperatingHourDto(
            com.ipscentir.appointments.domain.model.facility.FacilityOperatingHour hour
    ) {
        return new FacilityOperatingHourDTO(
                hour.getId(),
                hour.getDayOfWeek(),
                hour.getOpenTime(),
                hour.getCloseTime(),
                hour.isClosed()
        );
    }

    private FacilityResourceDTO toResourceDto(FacilityResource resource) {
        return new FacilityResourceDTO(
                resource.getId(),
                resource.getResourceType(),
                resource.getCode(),
                resource.getDisplayName(),
                resource.getCapacityUnits(),
                resource.isActive()
        );
    }
}
