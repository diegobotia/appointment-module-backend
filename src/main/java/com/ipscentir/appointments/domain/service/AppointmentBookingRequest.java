package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentBookingRequest(
        UUID patientId,
        String doctorId,
        String secondaryDoctorId,
        UUID scheduleId,
        Integer sedeId,
        LocalDate date,
        LocalTime time,
        AppointmentType type,
        String reason,
        BookingChannel bookingChannel,
        String n8nConversationId
) {
    public BookingChannel resolvedChannel() {
        return bookingChannel != null ? bookingChannel : BookingChannel.STAFF;
    }
}