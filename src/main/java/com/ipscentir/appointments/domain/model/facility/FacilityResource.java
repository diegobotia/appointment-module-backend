package com.ipscentir.appointments.domain.model.facility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "facility_resources", schema = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FacilityResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sede_id", nullable = false)
    private Integer sedeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private FacilityResourceType resourceType;

    @Column(nullable = false)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "capacity_units", nullable = false)
    @Builder.Default
    private int capacityUnits = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
