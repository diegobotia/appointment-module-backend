package com.ipscentir.appointments.domain.model.specialist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Specialist (Médico)
 *
 * Mapea a hc.medicos en el schema 'hc'.
 * Esta es una entidad de lectura principalmente (read-only in appointments context)
 * ya que el médico es mantenido en el sistema HC (health center).
 *
 * El appointments module no crea/modifica médicos, solo los referencia.
 */
@Entity
@Table(name = "medicos", schema = "hc")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Specialist {

    @Id
    @Column(name = "id", insertable = false, updatable = false)
    private String id;

    @Column(name = "registro", nullable = false)
    private String numeroMedico;

    @Column(name = "tipo_doc")
    private String tipoDoc;

    @Column(name = "num_doc")
    private String numDoc;

    @Column(name = "nombre", nullable = false)
    private String firstName;

    @Column(name = "apellido", nullable = false)
    private String lastName;

    @Column(name = "especialidad")
    private String specialty;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
