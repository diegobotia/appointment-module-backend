package com.ipscentir.appointments.application.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * RegisterPatientRequest
 * 
 * DTO para registrar un nuevo paciente en el sistema.
 * Recibe datos desde n8n o desde el formulario del frontend.
 * 
 * Estos datos se almacenan en:
 * - core.pacientes (para información clínica)
 * - core.profiles (para autenticación y perfil básico)
 * - core.contacto (para email y teléfono)
 * - core.direccion (para ubicación)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    description = "Request para registrar un nuevo paciente",
    example = """
        {
          "email": "juan@example.com",
          "nombres": "Juan",
          "apellidos": "Pérez García",
          "numIdentificacion": "1234567890",
          "codTipoIdentificacion": "CC",
          "fechaNacimiento": "1990-05-15",
          "idGenero": "uuid-genero",
          "idEstadoCivil": "uuid-estado-civil",
          "idOcupacion": "uuid-ocupacion",
          "idGrupoSanguineo": "uuid-grupo",
          "idEscolaridad": "uuid-escolaridad",
          "estrato": 3,
          "idPaisOrigen": 1,
          "telefono": "+57 300 1234567",
          "direccionDetalle": "Calle 10 #20-30",
          "codMunicipio": "050001",
          "codZonaTerritorial": "ZONA_1"
        }
        """
)
public class RegisterPatientRequest {

    // ========================================
    // DATOS DE AUTENTICACIÓN (Requeridos)
    // ========================================

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    @Schema(description = "Email del paciente", example = "juan@example.com")
    private String email;

    // ========================================
    // DATOS PERSONALES (Requeridos)
    // ========================================

    @NotBlank(message = "Nombres requeridos")
    @Size(min = 2, max = 100, message = "Nombres debe tener entre 2 y 100 caracteres")
    @Schema(description = "Nombres del paciente", example = "Juan")
    private String nombres;

    @NotBlank(message = "Apellidos requeridos")
    @Size(min = 2, max = 100, message = "Apellidos debe tener entre 2 y 100 caracteres")
    @Schema(description = "Apellidos del paciente", example = "Pérez García")
    private String apellidos;

    @NotBlank(message = "Número de identificación requerido")
    @Schema(description = "Número de identificación", example = "1234567890")
    private String numIdentificacion;

    @NotBlank(message = "Tipo de identificación requerido")
    @Schema(description = "Código del tipo de identificación (CC, TI, CE, etc)", example = "CC")
    private String codTipoIdentificacion;

    @NotNull(message = "Fecha de nacimiento requerida")
    @Past(message = "Fecha de nacimiento debe ser en el pasado")
    @Schema(description = "Fecha de nacimiento", example = "1990-05-15")
    private LocalDate fechaNacimiento;

    // ========================================
    // DATOS CLÍNICOS (Requeridos)
    // ========================================

    @NotBlank(message = "ID de género requerido")
    @Schema(description = "UUID del género del paciente", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idGenero;

    @NotBlank(message = "Estado civil requerido")
    @Schema(description = "UUID del estado civil", example = "550e8400-e29b-41d4-a716-446655440001")
    private String idEstadoCivil;

    @NotBlank(message = "Ocupación requerida")
    @Schema(description = "UUID de la ocupación", example = "550e8400-e29b-41d4-a716-446655440002")
    private String idOcupacion;

    @NotBlank(message = "Grupo sanguíneo requerido")
    @Schema(description = "UUID del grupo sanguíneo", example = "550e8400-e29b-41d4-a716-446655440003")
    private String idGrupoSanguineo;

    @NotBlank(message = "Escolaridad requerida")
    @Schema(description = "UUID de la escolaridad", example = "550e8400-e29b-41d4-a716-446655440004")
    private String idEscolaridad;

    @NotNull(message = "Estrato requerido")
    @Min(value = 1, message = "Estrato debe ser entre 1 y 6")
    @Max(value = 6, message = "Estrato debe ser entre 1 y 6")
    @Schema(description = "Estrato socioeconómico (1-6)", example = "3")
    private Integer estrato;

    @NotNull(message = "País de origen requerido")
    @Schema(description = "ID del país de origen", example = "1")
    private Long idPaisOrigen;

    // ========================================
    // DATOS DE CONTACTO (Requeridos)
    // ========================================

    @NotBlank(message = "Teléfono requerido")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Teléfono debe ser válido")
    @Schema(description = "Teléfono del paciente", example = "+573001234567")
    private String telefono;

    // ========================================
    // DATOS DE DIRECCIÓN (Requeridos)
    // ========================================

    @NotBlank(message = "Detalles de dirección requeridos")
    @Size(min = 10, max = 255, message = "Dirección debe tener entre 10 y 255 caracteres")
    @Schema(description = "Detalles de la dirección", example = "Calle 10 #20-30")
    private String direccionDetalle;

    @NotBlank(message = "Código de municipio requerido")
    @Schema(description = "Código del municipio", example = "050001")
    private String codMunicipio;

    @NotBlank(message = "Código de zona territorial requerido")
    @Schema(description = "Código de zona territorial", example = "ZONA_1")
    private String codZonaTerritorial;

    // ========================================
    // DATOS OPCIONALES
    // ========================================

    @Size(max = 100, message = "Barrio no puede exceder 100 caracteres")
    @Schema(description = "Barrio (opcional)", example = "Centro")
    private String barrio;
}
