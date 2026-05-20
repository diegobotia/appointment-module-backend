package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PqrsJpaRepository extends JpaRepository<Pqrs, UUID> {

    Optional<Pqrs> findByRadicado(String radicado);

    @Query("SELECT COUNT(p) FROM Pqrs p WHERE EXTRACT(YEAR FROM p.createdAt) = :year")
    long countByYearCreated(@Param("year") int year);

    long countByStatus(PqrsStatus status);

    @Query("""
            SELECT p FROM Pqrs p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:tipo IS NULL OR p.tipo = :tipo)
            ORDER BY p.createdAt DESC
            """)
    List<Pqrs> search(
            @Param("status") PqrsStatus status,
            @Param("tipo") PqrsType tipo,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(p) FROM Pqrs p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:tipo IS NULL OR p.tipo = :tipo)
            """)
    long countSearch(@Param("status") PqrsStatus status, @Param("tipo") PqrsType tipo);
}
