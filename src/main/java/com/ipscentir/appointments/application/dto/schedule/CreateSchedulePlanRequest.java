package com.ipscentir.appointments.application.dto.schedule;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateSchedulePlanRequest(
        @NotNull @JsonAlias("specialistId") String medicoId,
        @NotNull Integer sedeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty List<@Valid CreateSchedulePlanSlotRequest> slots
) {
}
