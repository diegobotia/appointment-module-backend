package com.ipscentir.appointments.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactoRepository extends JpaRepository<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Contacto, UUID> {
    Optional<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Contacto> findByEmail(String email);
    Optional<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Contacto> findByTelefono(String telefono);
    boolean existsByEmail(String email);
}
