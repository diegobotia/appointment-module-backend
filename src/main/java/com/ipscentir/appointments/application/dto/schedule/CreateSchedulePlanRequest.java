package com.ipscentir.appointments.application.dto.schedule;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSchedulePlanRequest(
        @NotNull @JsonAlias("specialistId") String medicoId,
        @Min(2020) @Max(2100) int planYear,
        @Min(1) @Max(4) int planQuarter,
        @NotEmpty List<@Valid CreateSchedulePlanSlotRequest> slots
) {
}
