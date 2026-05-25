package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read-only access to {@code hc.medicos}. Specialists are maintained by the HC system.
 */
@Repository
public interface SpecialistJpaRepository extends JpaRepository<Specialist, String> {

    @Query(value = """
            SELECT m.*
            FROM hc.medicos m
            WHERE CAST(m.id AS text) = :id
            """, nativeQuery = true)
    Optional<Specialist> findByIdText(@Param("id") String id);

    @Query(value = """
            SELECT m.*
            FROM hc.medicos m
            WHERE CAST(m.id AS text) IN (:ids)
            """, nativeQuery = true)
    List<Specialist> findAllByIdTextIn(@Param("ids") Collection<String> ids);

    @Query(value = """
            SELECT m.*
            FROM hc.medicos m
            WHERE LOWER(CONCAT(m.nombre, ' ', m.apellido)) = LOWER(:fullName)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Specialist> findByFullName(@Param("fullName") String fullName);

    Optional<Specialist> findByNumeroMedico(String numeroMedico);

    List<Specialist> findAllByActiveTrue();

    @Query(value = """
                SELECT DISTINCT m.*
                FROM hc.medicos m
                LEFT JOIN hc.medico_especialidades me
                                                                         ON CAST(me.medico_id AS text) = CAST(m.id AS text)
                        AND me.activo = true
                WHERE (:active IS NULL OR m.activo = :active)
                  AND (:numDoc IS NULL OR m.num_doc = :numDoc)
                  AND (:registro IS NULL OR m.registro = :registro)
                  AND (
                          :specialty IS NULL
                          OR LOWER(me.especialidad) = LOWER(:specialty)
                  )
                  AND (
                          :q IS NULL
                          OR LOWER(CONCAT(m.nombre, ' ', m.apellido)) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(m.apellido) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
                ORDER BY m.apellido, m.nombre
                """,
                countQuery = """
                SELECT COUNT(DISTINCT m.id)
                FROM hc.medicos m
                LEFT JOIN hc.medico_especialidades me
                             ON CAST(me.medico_id AS text) = CAST(m.id AS text)
                        AND me.activo = true
                WHERE (:active IS NULL OR m.activo = :active)
                  AND (:numDoc IS NULL OR m.num_doc = :numDoc)
                  AND (:registro IS NULL OR m.registro = :registro)
                  AND (
                          :specialty IS NULL
                          OR LOWER(me.especialidad) = LOWER(:specialty)
                  )
                  AND (
                          :q IS NULL
                          OR LOWER(CONCAT(m.nombre, ' ', m.apellido)) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(m.apellido) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
                """,
                nativeQuery = true)
    Page<Specialist> search(
            @Param("q") String q,
            @Param("numDoc") String numDoc,
            @Param("registro") String registro,
            @Param("specialty") String specialty,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
