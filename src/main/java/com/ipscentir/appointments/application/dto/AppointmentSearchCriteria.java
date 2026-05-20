package com.ipscentir.appointments.application.dto;

import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;

import java.time.LocalDate;
import java.util.UUID;

public record AppointmentSearchCriteria(
        UUID facilityId,
        String doctorId,
        UUID patientId,
        AppointmentStatus status,
        LocalDate fromDate,
        LocalDate toDate
) {
}
