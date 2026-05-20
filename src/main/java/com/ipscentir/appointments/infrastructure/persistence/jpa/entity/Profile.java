package com.ipscentir.appointments.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles", schema = "core")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    private UUID id;  // Mismo ID que auth.users

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "name")
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password_change_required")
    private Boolean passwordChangeRequired;

    @Column(name = "esta_activo")
    @Builder.Default
    private Boolean estaActivo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
