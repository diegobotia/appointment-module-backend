package com.ipscentir.appointments.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelAppointmentCommand(
        @NotBlank String reason
) {
}
