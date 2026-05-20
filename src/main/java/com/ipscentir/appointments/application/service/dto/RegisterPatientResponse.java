package com.ipscentir.appointments.application.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RegisterPatientResponse
 * 
 * DTO que devuelve la respuesta después de registrar un paciente exitosamente.
 * Contiene los IDs generados y la información confirmada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    description = "Response del registro de paciente",
    example = """
        {
          "patientId": "550e8400-e29b-41d4-a716-446655440099",
          "profileId": "550e8400-e29b-41d4-a716-446655440100",
          "email": "juan@example.com",
          "nombres": "Juan",
          "apellidos": "Pérez García",
          "message": "Paciente registrado exitosamente",
          "createdAt": "2026-05-16T10:30:00",
          "nextSteps": "El paciente puede ya agendar citas a través del portal"
        }
        """
)
public class RegisterPatientResponse {

    @Schema(
        description = "UUID del paciente creado en core.pacientes",
        example = "550e8400-e29b-41d4-a716-446655440099"
    )
    private String patientId;

    @Schema(
        description = "UUID del perfil creado en core.profiles",
        example = "550e8400-e29b-41d4-a716-446655440100"
    )
    private String profileId;

    @Schema(
        description = "Email del paciente registrado",
        example = "juan@example.com"
    )
    private String email;

    @Schema(
        description = "Nombres del paciente",
        example = "Juan"
    )
    private String nombres;

    @Schema(
        description = "Apellidos del paciente",
        example = "Pérez García"
    )
    private String apellidos;

    @Schema(
        description = "Mensaje de confirmación",
        example = "Paciente registrado exitosamente"
    )
    private String message;

    @Schema(
        description = "Timestamp de creación",
        example = "2026-05-16T10:30:00"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "Instrucciones para los próximos pasos",
        example = "El paciente puede ya agendar citas a través del portal"
    )
    private String nextSteps;
}
