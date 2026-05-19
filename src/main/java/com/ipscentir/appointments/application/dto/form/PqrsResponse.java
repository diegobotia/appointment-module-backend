package com.ipscentir.appointments.application.dto.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record PqrsResponse(
        String radicado,
        String status,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime createdAt,
        String cedula
) {
}
