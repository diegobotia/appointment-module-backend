package com.ipscentir.appointments.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
 

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "specialist_metadata", schema = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialistMetadata {

    @Id
    private UUID profileId;  // FK a core.profiles.id

    @Column(name = "specialties_json", columnDefinition = "jsonb")
    private String specialtiesJson;  // JSON: {"primary": "FISIOTERAPIA", "secondary": [...]}

    @Column(name = "max_patients_per_slot", nullable = false)
    private Integer maxPatientsPerSlot;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "synced_from_hc", nullable = false)
    private Boolean syncedFromHc;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
