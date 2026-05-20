package com.ipscentir.appointments.application.dto.schedule;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PublishSchedulePlanRequest(
        @NotNull UUID facilityId
) {
}
