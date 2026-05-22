package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.schedule.Schedule;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository {

    Optional<Schedule> findByDoctorIdAndSedeIdAndDayOfWeek(String doctorId, Integer sedeId, DayOfWeek dayOfWeek);

    java.util.List<Schedule> findBySedeIdAndDayOfWeek(Integer sedeId, DayOfWeek dayOfWeek);

    java.util.List<Schedule> findByDoctorIdAndSedeId(String doctorId, Integer sedeId);

    Optional<Schedule> findByDoctorIdAndDayOfWeek(String doctorId, DayOfWeek dayOfWeek);
    
    Schedule save(Schedule schedule);
    
    Optional<Schedule> findById(UUID id);
}
