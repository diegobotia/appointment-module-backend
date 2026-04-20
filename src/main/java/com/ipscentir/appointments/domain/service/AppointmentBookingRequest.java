package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.AppointmentType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentBookingRequest(
        UUID patientId,
        UUID doctorId,
        UUID secondaryDoctorId,
        UUID scheduleId,
        UUID facilityId,
        LocalDate date,
        LocalTime time,
        AppointmentType type,
        String reason
) {}