package com.ipscentir.appointments.application.dto.specialist;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignSpecialtyRequest(
        @NotNull String specialtyCode
) {
}