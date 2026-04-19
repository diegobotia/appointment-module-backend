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
            UUID specialistId,
            int planYear,
            int planQuarter
    );

    List<SchedulePlan> findBySpecialistIdOrderByPlanYearDescPlanQuarterDescVersionNumberDesc(UUID specialistId);

    Optional<SchedulePlan> findBySpecialistIdAndPlanYearAndPlanQuarterAndActiveVersionTrue(
            UUID specialistId,
            int planYear,
            int planQuarter
    );

    @Query("SELECT COALESCE(MAX(sp.versionNumber), 0) FROM SchedulePlan sp WHERE sp.specialist.id = :specialistId AND sp.planYear = :planYear AND sp.planQuarter = :planQuarter")
    int findMaxVersionBySpecialistAndPeriod(UUID specialistId, int planYear, int planQuarter);
}
