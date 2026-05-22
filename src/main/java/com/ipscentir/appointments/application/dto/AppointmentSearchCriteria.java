package com.ipscentir.appointments.application.dto;

import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;

import java.time.LocalDate;
import java.util.UUID;

public record AppointmentSearchCriteria(
        Integer sedeId,
        String doctorId,
        UUID patientId,
        AppointmentStatus status,
        BookingChannel bookingChannel,
        LocalDate fromDate,
        LocalDate toDate
) {
}
