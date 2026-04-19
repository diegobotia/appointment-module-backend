package com.ipscentir.appointments.domain.model.schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class AvailableSlot {
    private final LocalDate date;
    private final LocalTime time;
    private final Integer durationMinutes;
}
