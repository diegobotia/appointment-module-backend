package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.schedule.Schedule;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository {

    Optional<Schedule> findByDoctorIdAndFacilityIdAndDayOfWeek(String doctorId, UUID facilityId, DayOfWeek dayOfWeek);

    java.util.List<Schedule> findByFacilityIdAndDayOfWeek(UUID facilityId, DayOfWeek dayOfWeek);
    
        Optional<Schedule> findByDoctorIdAndDayOfWeek(String doctorId, DayOfWeek dayOfWeek);
    
    Schedule save(Schedule schedule);
    
    Optional<Schedule> findById(UUID id);
}
