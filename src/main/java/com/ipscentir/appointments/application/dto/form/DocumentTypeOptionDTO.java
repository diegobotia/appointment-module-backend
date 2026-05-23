package com.ipscentir.appointments.application.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de documento DIAN para el formulario de registro")
public record DocumentTypeOptionDTO(
        @Schema(description = "Código DIAN (FK en core.pacientes)", example = "13")
        String codigo,
        @Schema(description = "Descripción legible para el usuario", example = "Cédula de ciudadanía")
        String descripcion
) {
}
