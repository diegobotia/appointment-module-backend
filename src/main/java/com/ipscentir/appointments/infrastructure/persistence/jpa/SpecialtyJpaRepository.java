package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.specialty.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecialtyJpaRepository extends JpaRepository<Specialty, UUID> {

    Optional<Specialty> findByCode(String code);

    List<Specialty> findAllByActiveTrueOrderByDisplayNameAsc();
}