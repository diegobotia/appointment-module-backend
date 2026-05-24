package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to {@code hc.medicos}. Specialists are maintained by the HC system.
 */
@Repository
public interface SpecialistJpaRepository extends JpaRepository<Specialist, String> {

    @Query(value = "SELECT * FROM hc.medicos WHERE CAST(id AS text) = :id", nativeQuery = true)
    Optional<Specialist> findById(@Param("id") String id);

    Optional<Specialist> findByNumeroMedico(String numeroMedico);

    List<Specialist> findAllByActiveTrue();

    @Query("""
            SELECT s FROM Specialist s
            WHERE (:active IS NULL OR s.active = :active)
              AND (:numDoc IS NULL OR s.numDoc = :numDoc)
              AND (:registro IS NULL OR s.numeroMedico = :registro)
              AND (:specialty IS NULL OR LOWER(s.specialty) = LOWER(:specialty))
              AND (
                    :q IS NULL
                    OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Specialist> search(
            @Param("q") String q,
            @Param("numDoc") String numDoc,
            @Param("registro") String registro,
            @Param("specialty") String specialty,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
