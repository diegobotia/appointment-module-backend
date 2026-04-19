package com.ipscentir.appointments.application.dto.integration.n8n;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record N8nPatientAvailabilityRequest(
        @NotNull UUID doctorId,
        @NotNull LocalDate date
) {}