package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.facility.AppointmentResourceAllocation;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentResourceAllocationJpaRepository extends JpaRepository<AppointmentResourceAllocation, UUID> {

        Optional<AppointmentResourceAllocation> findByAppointmentIdAndReleasedAtIsNull(UUID appointmentId);

        Optional<AppointmentResourceAllocation> findByAppointmentId(UUID appointmentId);

        boolean existsByCapacitySessionKeyAndReleasedAtIsNull(String capacitySessionKey);

        @Query("""
                        SELECT COUNT(DISTINCT a.capacitySessionKey)
                        FROM AppointmentResourceAllocation a
                        WHERE a.sedeId = :sedeId
                          AND a.resourceType = :resourceType
                          AND a.appointmentDate = :appointmentDate
                          AND a.releasedAt IS NULL
                          AND a.startTime < :endTime
                          AND a.endTime > :startTime
                        """)
        long countOccupiedCapacityUnits(
                        @Param("sedeId") Integer sedeId,
                        @Param("resourceType") FacilityResourceType resourceType,
                        @Param("appointmentDate") LocalDate appointmentDate,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime);

        @Query("""
                        SELECT COUNT(DISTINCT a.capacitySessionKey)
                        FROM AppointmentResourceAllocation a
                        WHERE a.sedeId = :sedeId
                          AND a.resourceType = :resourceType
                          AND a.appointmentDate = :appointmentDate
                          AND a.releasedAt IS NULL
                          AND a.startTime < :endTime
                          AND a.endTime > :startTime
                          AND a.appointmentId <> :excludeAppointmentId
                        """)
        long countOccupiedCapacityUnitsExcludingAppointment(
                        @Param("sedeId") Integer sedeId,
                        @Param("resourceType") FacilityResourceType resourceType,
                        @Param("appointmentDate") LocalDate appointmentDate,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime,
                        @Param("excludeAppointmentId") UUID excludeAppointmentId);

        @Query("""
                        SELECT COUNT(DISTINCT a.id)
                        FROM AppointmentResourceAllocation a
                        WHERE a.facilityResourceId = :facilityResourceId
                          AND a.appointmentDate = :appointmentDate
                          AND a.releasedAt IS NULL
                          AND a.startTime < :endTime
                          AND a.endTime > :startTime
                        """)
        long countOccupiedForResource(
                        @Param("facilityResourceId") UUID facilityResourceId,
                        @Param("appointmentDate") LocalDate appointmentDate,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime);

        @Query("""
                        SELECT COUNT(DISTINCT a.id)
                        FROM AppointmentResourceAllocation a
                        WHERE a.facilityResourceId = :facilityResourceId
                          AND a.appointmentDate = :appointmentDate
                          AND a.releasedAt IS NULL
                          AND a.startTime < :endTime
                          AND a.endTime > :startTime
                          AND a.appointmentId <> :excludeAppointmentId
                        """)
        long countOccupiedForResourceExcluding(
                        @Param("facilityResourceId") UUID facilityResourceId,
                        @Param("appointmentDate") LocalDate appointmentDate,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime,
                        @Param("excludeAppointmentId") UUID excludeAppointmentId);
}
