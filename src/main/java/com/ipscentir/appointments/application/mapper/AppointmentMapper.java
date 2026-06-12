package com.ipscentir.appointments.application.mapper;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    @Mapping(source = "doctorId", target = "medicoId")
    @Mapping(source = "additionalDoctorIds", target = "additionalMedicoIds")
    @Mapping(target = "medicoDisplayName", ignore = true)
    @Mapping(target = "patientDisplayName", ignore = true)
    @Mapping(target = "administrative", expression = "java(appointment.isAdministrative())")
    AppointmentDTO toDto(Appointment appointment);
}
