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
    public Optional<Schedule> findByDoctorIdAndSedeIdAndDayOfWeek(String doctorId, Integer sedeId, DayOfWeek dayOfWeek) {
        return jpaRepository.findByDoctorIdAndSedeIdAndDayOfWeekWithBlocks(doctorId, sedeId, dayOfWeek);
    }

    @Override
    public List<Schedule> findBySedeIdAndDayOfWeek(Integer sedeId, DayOfWeek dayOfWeek) {
        return jpaRepository.findBySedeIdAndDayOfWeekWithBlocks(sedeId, dayOfWeek);
    }

    @Override
    public List<Schedule> findByDoctorIdAndSedeId(String doctorId, Integer sedeId) {
        return jpaRepository.findByDoctorIdAndSedeIdWithBlocks(doctorId, sedeId);
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
