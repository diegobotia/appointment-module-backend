package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchedulePlanJpaRepository extends JpaRepository<SchedulePlan, UUID> {

    @Query("""
            SELECT DISTINCT sp FROM SchedulePlan sp
            LEFT JOIN FETCH sp.slots
            WHERE sp.id = :id
            """)
    Optional<SchedulePlan> findWithSlotsById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT sp FROM SchedulePlan sp
            LEFT JOIN FETCH sp.blocks
            WHERE sp.id = :id
            """)
    Optional<SchedulePlan> findWithBlocksById(@Param("id") UUID id);

    default Optional<SchedulePlan> findWithSlotsAndBlocksById(UUID id) {
        Optional<SchedulePlan> withSlots = findWithSlotsById(id);
        if (withSlots.isEmpty()) {
            return Optional.empty();
        }
        findWithBlocksById(id);
        return withSlots;
    }

    List<SchedulePlan> findBySpecialistIdAndStartDateAndEndDateOrderByVersionNumberDesc(
            String specialistId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<SchedulePlan> findBySpecialistIdOrderByStartDateDescEndDateDescVersionNumberDesc(String specialistId);

    Optional<SchedulePlan> findBySpecialistIdAndStartDateAndEndDateAndActiveVersionTrue(
            String specialistId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
            SELECT COALESCE(MAX(sp.versionNumber), 0) FROM SchedulePlan sp
            WHERE sp.specialistId = :specialistId
              AND sp.startDate = :startDate
              AND sp.endDate = :endDate
            """)
    int findMaxVersionBySpecialistAndPeriod(
            @Param("specialistId") String specialistId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT sp FROM SchedulePlan sp
            WHERE sp.specialistId = :specialistId
              AND sp.activeVersion = true
              AND sp.startDate <= :endDate
              AND sp.endDate >= :startDate
            """)
    List<SchedulePlan> findOverlappingActivePlans(
            @Param("specialistId") String specialistId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT DISTINCT sp FROM SchedulePlan sp
            LEFT JOIN FETCH sp.slots
            WHERE sp.activeVersion = true
              AND sp.published = true
              AND sp.startDate <= :endDate
              AND sp.endDate >= :startDate
            """)
    List<SchedulePlan> findAllOverlappingActivePlans(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
