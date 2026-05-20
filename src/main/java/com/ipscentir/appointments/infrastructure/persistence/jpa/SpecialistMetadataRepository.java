package com.ipscentir.appointments.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecialistMetadataRepository extends JpaRepository<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.SpecialistMetadata, UUID> {
    Optional<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.SpecialistMetadata> findByProfileId(UUID profileId);
    List<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.SpecialistMetadata> findByIsActiveTrue();
    List<com.ipscentir.appointments.infrastructure.persistence.jpa.entity.SpecialistMetadata> findBySyncedFromHcFalse();
}
