package com.ipscentir.appointments.application.dto.admin;

import java.util.UUID;

public record SedeSummaryDTO(
        Integer id,
        String nombre,
        UUID direccion,
        String matriculaMercantil
) {
}
