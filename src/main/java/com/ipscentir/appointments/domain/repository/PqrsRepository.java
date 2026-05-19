package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import java.util.Optional;
import java.util.UUID;

public interface PqrsRepository {
    Pqrs save(Pqrs pqrs);
    Optional<Pqrs> findById(UUID id);
    Optional<Pqrs> findByRadicado(String radicado);
    long countByYearCreated(int year);
}
