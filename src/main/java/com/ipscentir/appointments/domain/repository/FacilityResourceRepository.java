package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.facility.FacilityResource;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacilityResourceRepository {

    Optional<FacilityResource> findById(UUID id);

    List<FacilityResource> findActiveBySedeIdOrderByTypeAndCode(Integer sedeId);

    long countActiveBySedeId(Integer sedeId);

    long countActiveBySedeIdAndResourceType(Integer sedeId, FacilityResourceType resourceType);

    List<FacilityResource> saveAll(Iterable<FacilityResource> resources);
}
