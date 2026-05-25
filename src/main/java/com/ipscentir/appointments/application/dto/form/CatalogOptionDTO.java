package com.ipscentir.appointments.application.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Opción de catálogo usada por los formularios de pacientes")
public record CatalogOptionDTO(
        @Schema(description = "Identificador del catálogo", example = "82c11edc-6662-4098-95f8-6f21f9f07263")
        String id,
        @Schema(description = "Etiqueta legible del catálogo", example = "Femenino")
        String label
) {
}
