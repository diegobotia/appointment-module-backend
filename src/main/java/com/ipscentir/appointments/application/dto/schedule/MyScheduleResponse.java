package com.ipscentir.appointments.application.dto.schedule;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.ScheduleDTO;

import java.time.LocalDate;
import java.util.List;

public record MyScheduleResponse(
        String doctorId,
        LocalDate fromDate,
        LocalDate toDate,
        List<ScheduleDTO> scheduleTemplates,
        List<AppointmentDTO> appointments
) {
}
