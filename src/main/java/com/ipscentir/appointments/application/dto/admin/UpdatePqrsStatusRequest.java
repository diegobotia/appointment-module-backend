package com.ipscentir.appointments.application.dto.admin;

import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePqrsStatusRequest(
        @NotNull PqrsStatus status
) {
}
