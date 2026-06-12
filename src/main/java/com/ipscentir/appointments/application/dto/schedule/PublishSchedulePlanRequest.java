package com.ipscentir.appointments.application.dto.schedule;

import jakarta.validation.constraints.NotNull;

public record PublishSchedulePlanRequest(
        @NotNull Integer sedeId
) {
}
