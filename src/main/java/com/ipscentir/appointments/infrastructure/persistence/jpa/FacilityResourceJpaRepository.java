package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.facility.FacilityResource;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FacilityResourceJpaRepository extends JpaRepository<FacilityResource, UUID> {

    List<FacilityResource> findBySedeIdAndActiveTrueOrderByResourceTypeAscCodeAsc(Integer sedeId);

    long countBySedeIdAndActiveTrue(Integer sedeId);

    long countBySedeIdAndResourceTypeAndActiveTrue(Integer sedeId, FacilityResourceType resourceType);
}
