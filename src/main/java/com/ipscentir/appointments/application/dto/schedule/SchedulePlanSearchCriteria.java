package com.ipscentir.appointments.application.dto.schedule;

public record SchedulePlanSearchCriteria(
        String specialistId,
        Integer year,
        Integer quarter,
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
