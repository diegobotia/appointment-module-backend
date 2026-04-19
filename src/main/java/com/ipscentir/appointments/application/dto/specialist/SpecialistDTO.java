package com.ipscentir.appointments.application.dto.specialist;

import java.util.UUID;

public record SpecialistDTO(
        UUID id,
        String firstName,
        String lastName,
        String email,
        boolean active
) {
}
