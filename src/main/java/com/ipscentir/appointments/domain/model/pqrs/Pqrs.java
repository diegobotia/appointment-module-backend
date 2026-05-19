package com.ipscentir.appointments.domain.model.pqrs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pqrs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Pqrs extends AbstractAggregateRoot<Pqrs> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String cedula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PqrsType tipo;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(nullable = false)
    private String correo;

    private String nombres;

    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PqrsStatus status = PqrsStatus.CREADO;

    @Column(nullable = false, unique = true)
    private String radicado;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(insertable = false)
    private LocalDateTime updatedAt;

    public static Pqrs create(
            String cedula,
            PqrsType tipo,
            String descripcion,
            String correo,
            String nombres,
            String telefono,
            String radicado) {
        return Pqrs.builder()
                .cedula(cedula)
                .tipo(tipo)
                .descripcion(descripcion)
                .correo(correo)
                .nombres(nombres)
                .telefono(telefono)
                .radicado(radicado)
                .status(PqrsStatus.CREADO)
                .build();
    }
}
