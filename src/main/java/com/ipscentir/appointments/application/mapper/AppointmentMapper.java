package com.ipscentir.appointments.application.mapper;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    AppointmentDTO toDto(Appointment appointment);
}
