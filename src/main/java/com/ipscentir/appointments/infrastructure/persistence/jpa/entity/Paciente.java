package com.ipscentir.appointments.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pacientes", schema = "core")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "num_identificacion", nullable = false, unique = true)
    private String numIdentificacion;

    @Column(name = "cod_tipo_identificacion", nullable = false)
    private String codTipoIdentificacion;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "id_genero", nullable = false)
    private UUID idGenero;

    @Column(name = "id_estado_civil", nullable = false)
    private UUID idEstadoCivil;

    @Column(name = "id_ocupacion", nullable = false)
    private UUID idOcupacion;

    @Column(name = "id_direccion", nullable = false)
    private UUID idDireccion;

    @Column(name = "id_contacto", nullable = false)
    private UUID idContacto;

    @Column(name = "id_grupo_sanguineo", nullable = false)
    private UUID idGrupoSanguineo;

    @Column(name = "id_escolaridad", nullable = false)
    private UUID idEscolaridad;

    @Column(name = "estrato", nullable = false)
    private Short estrato;

    @Column(name = "id_pais_origen", nullable = false)
    private Long idPaisOrigen;
}
