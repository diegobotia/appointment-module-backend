package com.ipscentir.appointments.domain.model.sede;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "sede", schema = "core")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Sede {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    private UUID direccion;

    @Column(name = "matricula_mercantil")
    private String matriculaMercantil;
}
