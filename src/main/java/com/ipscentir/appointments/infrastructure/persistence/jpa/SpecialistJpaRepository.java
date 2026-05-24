package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SpecialistJpaRepository - Read-only access to hc.medicos
 *
 * Maps to hc.medicos table. Specialists are managed by the HC (health center)
 * system and are read-only in the appointments context.
 */
@Repository
public interface SpecialistJpaRepository extends JpaRepository<Specialist, String> {

    @Query(value = "SELECT * FROM hc.medicos WHERE CAST(id AS text) = :id", nativeQuery = true)
    Optional<Specialist> findById(@Param("id") String id);

    Optional<Specialist> findByNumeroMedico(String numeroMedico);

    List<Specialist> findAllByActiveTrue();
}
