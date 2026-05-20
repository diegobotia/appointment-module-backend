package com.ipscentir.appointments.application.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos del formulario público de registro de paciente (solo core.pacientes)")
public class CreatePatientRegistrationRequest {

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    private String email;

    @NotBlank(message = "Nombres requeridos")
    @Size(min = 2, max = 100)
    private String nombres;

    @NotBlank(message = "Apellidos requeridos")
    @Size(min = 2, max = 100)
    private String apellidos;

    @NotBlank(message = "Número de identificación requerido")
    private String numIdentificacion;

    @NotBlank(message = "Tipo de identificación requerido")
    private String codTipoIdentificacion;

    @NotNull(message = "Fecha de nacimiento requerida")
    @Past(message = "Fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "ID de género requerido")
    private String idGenero;

    @NotBlank(message = "Estado civil requerido")
    private String idEstadoCivil;

    @NotBlank(message = "Ocupación requerida")
    private String idOcupacion;

    @NotBlank(message = "Grupo sanguíneo requerido")
    private String idGrupoSanguineo;

    @NotBlank(message = "Escolaridad requerida")
    private String idEscolaridad;

    @NotNull(message = "Estrato requerido")
    @Min(1)
    @Max(6)
    private Integer estrato;

    @NotNull(message = "País de origen requerido")
    private Long idPaisOrigen;

    @NotBlank(message = "Teléfono requerido")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Teléfono debe ser válido")
    private String telefono;

    @NotBlank(message = "Detalles de dirección requeridos")
    @Size(min = 10, max = 255)
    private String direccionDetalle;

    @NotBlank(message = "Código de municipio requerido")
    private String codMunicipio;

    @NotBlank(message = "Código de zona territorial requerido")
    private String codZonaTerritorial;

    @Size(max = 100)
    private String barrio;
}
