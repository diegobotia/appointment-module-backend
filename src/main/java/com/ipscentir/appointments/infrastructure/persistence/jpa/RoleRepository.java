package com.ipscentir.appointments.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Role, UUID> {
    Optional<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Role> findByNombre(String nombre);
}
