package com.ipscentir.appointments.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "CoreRole")
@Table(name = "roles", schema = "core")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;
}
