package com.ipscentir.appointments.application.dto.form;

import com.ipscentir.appointments.application.validation.ValidColombiaCedula;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePqrsRequest(
        @NotBlank(message = "La cédula no puede estar vacía")
        @ValidColombiaCedula
        String cedula,

        @NotBlank(message = "El tipo de PQRS es requerido")
        @Pattern(regexp = "^(PETICION|QUEJA|RECLAMO|SUGERENCIA)$", 
                message = "El tipo debe ser uno de: PETICION, QUEJA, RECLAMO, SUGERENCIA")
        String tipo,

        @NotBlank(message = "La descripción no puede estar vacía")
        @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
        String descripcion,

        @NotBlank(message = "El correo no puede estar vacío")
        @Email(message = "El correo debe ser válido")
        String correo,

        String nombres,

        String telefono
) {
}
