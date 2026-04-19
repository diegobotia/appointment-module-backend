package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.ScheduleDTO;
import com.ipscentir.appointments.application.mapper.ScheduleMapper;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleApplicationService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;

    public ScheduleDTO getScheduleForDoctor(UUID doctorId, DayOfWeek dayOfWeek) {
        Schedule schedule = scheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek)
                .orElseThrow(() -> new IllegalArgumentException("No schedule found for this doctor on the specified day"));
        
        return scheduleMapper.toDto(schedule);
    }
}
