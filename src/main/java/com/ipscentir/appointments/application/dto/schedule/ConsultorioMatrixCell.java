package com.ipscentir.appointments.application.dto.schedule;

import java.time.LocalTime;
import java.util.UUID;

public record ConsultorioMatrixCell(
        String medicoId,
        String medicoName,
        String specialty,
        LocalTime startTime,
        LocalTime endTime,
        UUID schedulePlanId,
        int versionNumber
) {
}
