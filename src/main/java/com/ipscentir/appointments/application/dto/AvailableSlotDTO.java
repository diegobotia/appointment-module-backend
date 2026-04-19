package com.ipscentir.appointments.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableSlotDTO(
        LocalDate date,
        LocalTime time,
        Integer durationMinutes
) {}
