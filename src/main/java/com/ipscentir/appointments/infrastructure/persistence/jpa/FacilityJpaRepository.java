package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.facility.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface FacilityJpaRepository extends JpaRepository<Facility, UUID> {

    Optional<Facility> findByCode(String code);

    List<Facility> findByActiveTrue();
}
