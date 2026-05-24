package com.ipscentir.appointments.application.dto.schedule;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.ScheduleDTO;

import java.time.LocalDate;
import java.util.List;

public record MyScheduleResponse(
        @JsonAlias("doctorId")
        String medicoId,
        LocalDate fromDate,
        LocalDate toDate,
        List<ScheduleDTO> scheduleTemplates,
        List<AppointmentDTO> appointments
) {
}
