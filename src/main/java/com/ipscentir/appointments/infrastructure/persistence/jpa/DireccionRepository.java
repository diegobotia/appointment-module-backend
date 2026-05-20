package com.ipscentir.appointments.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DireccionRepository extends JpaRepository<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Direccion, UUID> {
    // Métodos custom si aplican en el futuro
}
