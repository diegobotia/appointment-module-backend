package com.ipscentir.appointments.application.dto.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record UpdateSchedulePlanRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        List<@Valid CreateSchedulePlanSlotRequest> slots
) {
}
