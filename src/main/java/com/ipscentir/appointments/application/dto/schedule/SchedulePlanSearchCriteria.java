package com.ipscentir.appointments.application.dto.schedule;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDate;

public record SchedulePlanSearchCriteria(
        @JsonAlias("specialistId") String medicoId,
        LocalDate startDate,
        LocalDate endDate,
        Boolean published,
        Boolean activeVersion,
        int page,
        int size
) {
    public SchedulePlanSearchCriteria {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }
}
