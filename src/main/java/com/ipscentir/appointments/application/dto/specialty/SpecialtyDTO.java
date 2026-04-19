package com.ipscentir.appointments.application.dto.specialty;

import java.util.UUID;

public record SpecialtyDTO(
        UUID id,
        String code,
        String displayName,
        boolean active
) {
}