package com.ipscentir.appointments.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "direccion", schema = "core")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cod_municipio", nullable = false)
    private String codMunicipio;

    @Column(name = "cod_zona_territorial", nullable = false)
    private String codZonaTerritorial;

    @Column(name = "detalle", nullable = false)
    private String detalle;

    @Column(name = "barrio")
    private String barrio;
}
