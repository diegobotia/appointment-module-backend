package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpecialistJpaRepository extends JpaRepository<Specialist, UUID> {

    Optional<Specialist> findByEmail(String email);

    @EntityGraph(attributePaths = "specialties")
    Optional<Specialist> findWithSpecialtiesById(UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Page<Specialist> findDistinctBySpecialties_IdAndActiveTrue(UUID specialtyId, Pageable pageable);

    Page<Specialist> findDistinctBySpecialties_Id(UUID specialtyId, Pageable pageable);
}
