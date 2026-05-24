package com.ipscentir.appointments.application.mapper;

import com.ipscentir.appointments.application.dto.AvailableSlotDTO;
import com.ipscentir.appointments.application.dto.ScheduleDTO;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlot;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScheduleMapper {

    @Mapping(source = "doctorId", target = "medicoId")
    ScheduleDTO toDto(Schedule schedule);
    
    AvailableSlotDTO toDto(AvailableSlot availableSlot);
}
