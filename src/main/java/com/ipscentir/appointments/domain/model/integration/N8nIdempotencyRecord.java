package com.ipscentir.appointments.domain.model.integration;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "n8n_idempotency_keys",
        schema = "appointments",
        uniqueConstraints = @UniqueConstraint(name = "uq_n8n_idempotency_scope_key", columnNames = {"scope", "idempotency_key"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class N8nIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scope", nullable = false, length = 64)
    private String scope;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
