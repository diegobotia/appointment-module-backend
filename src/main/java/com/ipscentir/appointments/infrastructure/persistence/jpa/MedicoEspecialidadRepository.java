package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.MedicoEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicoEspecialidadRepository extends JpaRepository<MedicoEspecialidad, UUID> {

    List<MedicoEspecialidad> findByMedicoIdAndActivoTrueOrderByCreatedAtAsc(UUID medicoId);

    Optional<MedicoEspecialidad> findFirstByMedicoIdAndActivoTrueOrderByCreatedAtAsc(UUID medicoId);

    @Query("""
            SELECT DISTINCT me.especialidad
            FROM MedicoEspecialidad me
            WHERE me.medicoId = :medicoId
              AND me.activo = true
            ORDER BY me.especialidad ASC
            """)
    List<String> findActiveSpecialties(@Param("medicoId") UUID medicoId);
}