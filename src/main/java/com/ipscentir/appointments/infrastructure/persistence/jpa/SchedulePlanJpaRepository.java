package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchedulePlanJpaRepository extends JpaRepository<SchedulePlan, UUID> {

    Optional<SchedulePlan> findWithSlotsAndBlocksById(UUID id);

    List<SchedulePlan> findBySpecialistIdAndPlanYearAndPlanQuarterOrderByVersionNumberDesc(
            String specialistId,
            int planYear,
            int planQuarter
    );

    List<SchedulePlan> findBySpecialistIdOrderByPlanYearDescPlanQuarterDescVersionNumberDesc(String specialistId);

    Optional<SchedulePlan> findBySpecialistIdAndPlanYearAndPlanQuarterAndActiveVersionTrue(
            String specialistId,
            int planYear,
            int planQuarter
    );

        @Query("SELECT COALESCE(MAX(sp.versionNumber), 0) FROM SchedulePlan sp WHERE sp.specialistId = :specialistId AND sp.planYear = :planYear AND sp.planQuarter = :planQuarter")
    int findMaxVersionBySpecialistAndPeriod(String specialistId, int planYear, int planQuarter);
}
