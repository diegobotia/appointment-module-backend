package com.ipscentir.appointments.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile, UUID> {
    Optional<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile> findByEmail(String email);
    boolean existsByEmail(String email);
    // Removed incorrect derived query (Profile has roleId). Provide a typed method if needed.
    Optional<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile> findByIdAndRoleId(UUID id, UUID roleId);
}
