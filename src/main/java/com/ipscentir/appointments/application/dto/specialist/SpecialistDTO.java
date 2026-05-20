package com.ipscentir.appointments.application.dto.specialist;

public record SpecialistDTO(
        String id,
        String firstName,
        String lastName,
        boolean active
) {
}
