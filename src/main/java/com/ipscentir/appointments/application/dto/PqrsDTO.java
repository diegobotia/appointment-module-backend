package com.ipscentir.appointments.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsType;
import java.time.LocalDateTime;
import java.util.UUID;

public record PqrsDTO(
                UUID id,
                String cedula,
                PqrsType tipo,
                String descripcion,
                String correo,
                String nombres,
                String telefono,
                String radicado,
                PqrsStatus status,
                String metadata,
                @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime createdAt,
                @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime updatedAt) {
}
