package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduleRepositoryImpl implements ScheduleRepository {

    private final ScheduleJpaRepository jpaRepository;

    @Override
    public Optional<Schedule> findByDoctorIdAndFacilityIdAndDayOfWeek(String doctorId, UUID facilityId, DayOfWeek dayOfWeek) {
        return jpaRepository.findByDoctorIdAndFacilityIdAndDayOfWeekWithBlocks(doctorId, facilityId, dayOfWeek);
    }

    @Override
    public List<Schedule> findByFacilityIdAndDayOfWeek(UUID facilityId, DayOfWeek dayOfWeek) {
        return jpaRepository.findByFacilityIdAndDayOfWeekWithBlocks(facilityId, dayOfWeek);
    }

    @Override
    public List<Schedule> findByDoctorIdAndFacilityId(String doctorId, UUID facilityId) {
        return jpaRepository.findByDoctorIdAndFacilityIdWithBlocks(doctorId, facilityId);
    }

    @Override
    public Optional<Schedule> findByDoctorIdAndDayOfWeek(String doctorId, DayOfWeek dayOfWeek) {
        return jpaRepository.findByDoctorIdAndDayOfWeekWithBlocks(doctorId, dayOfWeek);
    }

    @Override
    public Schedule save(Schedule schedule) {
        return jpaRepository.save(schedule);
    }

    @Override
    public Optional<Schedule> findById(UUID id) {
        return jpaRepository.findById(id);
    }
}
