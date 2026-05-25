package com.ipscentir.appointments.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medico_especialidades", schema = "hc")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicoEspecialidad {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "medico_id", nullable = false)
    private UUID medicoId;

    @Column(name = "especialidad", nullable = false)
    private String especialidad;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}