package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.schedule.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleJpaRepository extends JpaRepository<Schedule, UUID> {

    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.blocks WHERE s.doctorId = :doctorId AND s.sedeId = :sedeId AND s.dayOfWeek = :dayOfWeek")
    Optional<Schedule> findByDoctorIdAndSedeIdAndDayOfWeekWithBlocks(
            @Param("doctorId") String doctorId,
            @Param("sedeId") Integer sedeId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

        @Query("SELECT DISTINCT s FROM Schedule s LEFT JOIN FETCH s.blocks WHERE s.sedeId = :sedeId AND s.dayOfWeek = :dayOfWeek AND s.isActive = true")
        List<Schedule> findBySedeIdAndDayOfWeekWithBlocks(
            @Param("sedeId") Integer sedeId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
        );
    
    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.blocks WHERE s.doctorId = :doctorId AND s.dayOfWeek = :dayOfWeek")
    Optional<Schedule> findByDoctorIdAndDayOfWeekWithBlocks(@Param("doctorId") String doctorId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT DISTINCT s FROM Schedule s LEFT JOIN FETCH s.blocks WHERE s.doctorId = :doctorId AND s.sedeId = :sedeId AND s.isActive = true")
    List<Schedule> findByDoctorIdAndSedeIdWithBlocks(
            @Param("doctorId") String doctorId,
            @Param("sedeId") Integer sedeId
    );
}
