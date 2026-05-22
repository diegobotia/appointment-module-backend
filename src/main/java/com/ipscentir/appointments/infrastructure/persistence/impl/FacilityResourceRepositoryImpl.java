package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.facility.FacilityResource;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import com.ipscentir.appointments.domain.repository.FacilityResourceRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.FacilityResourceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FacilityResourceRepositoryImpl implements FacilityResourceRepository {

    private final FacilityResourceJpaRepository jpaRepository;

    @Override
    public List<FacilityResource> findActiveBySedeIdOrderByTypeAndCode(Integer sedeId) {
        return jpaRepository.findBySedeIdAndActiveTrueOrderByResourceTypeAscCodeAsc(sedeId);
    }

    @Override
    public long countActiveBySedeId(Integer sedeId) {
        return jpaRepository.countBySedeIdAndActiveTrue(sedeId);
    }

    @Override
    public long countActiveBySedeIdAndResourceType(
            Integer sedeId,
            FacilityResourceType resourceType
    ) {
        return jpaRepository.countBySedeIdAndResourceTypeAndActiveTrue(sedeId, resourceType);
    }

    @Override
    public List<FacilityResource> saveAll(Iterable<FacilityResource> resources) {
        return jpaRepository.saveAll(resources);
    }
}
