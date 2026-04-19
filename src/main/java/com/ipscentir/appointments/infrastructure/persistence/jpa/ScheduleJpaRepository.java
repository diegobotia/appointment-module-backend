package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.schedule.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleJpaRepository extends JpaRepository<Schedule, UUID> {
    
    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.blocks WHERE s.doctorId = :doctorId AND s.dayOfWeek = :dayOfWeek")
    Optional<Schedule> findByDoctorIdAndDayOfWeekWithBlocks(@Param("doctorId") UUID doctorId, @Param("dayOfWeek") DayOfWeek dayOfWeek);
}
