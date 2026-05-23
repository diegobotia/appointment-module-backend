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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "appointment_resource_allocations", schema = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AppointmentResourceAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    @Column(name = "sede_id", nullable = false)
    private Integer sedeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private FacilityResourceType resourceType;

    @Column(name = "facility_resource_id")
    private UUID facilityResourceId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Clave de sesión concurrente: por cita en consultorio; por slot de terapia grupal en fisio/TO.
     */
    @Column(name = "capacity_session_key", nullable = false)
    private String capacitySessionKey;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public void release() {
        this.releasedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return releasedAt == null;
    }
}
