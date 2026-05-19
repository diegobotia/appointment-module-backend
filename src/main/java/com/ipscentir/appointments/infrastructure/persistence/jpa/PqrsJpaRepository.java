package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PqrsJpaRepository extends JpaRepository<Pqrs, UUID> {
    Optional<Pqrs> findByRadicado(String radicado);

    @Query("SELECT COUNT(p) FROM Pqrs p WHERE EXTRACT(YEAR FROM p.createdAt) = :year")
    long countByYearCreated(int year);
}
