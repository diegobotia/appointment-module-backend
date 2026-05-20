package com.ipscentir.appointments.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PacienteRepository extends JpaRepository<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente, UUID> {
    Optional<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente> findByNumIdentificacion(String numIdentificacion);
    boolean existsByNumIdentificacion(String numIdentificacion);
    // Paciente entity does not have email; removed incorrect derived query
}
