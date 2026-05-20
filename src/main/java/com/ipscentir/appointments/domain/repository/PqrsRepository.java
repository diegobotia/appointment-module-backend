package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PqrsRepository {
    Pqrs save(Pqrs pqrs);

    Optional<Pqrs> findById(UUID id);

    Optional<Pqrs> findByRadicado(String radicado);

    long countByYearCreated(int year);

    long countByStatus(PqrsStatus status);

    List<Pqrs> search(PqrsStatus status, PqrsType tipo, int offset, int limit);

    long countSearch(PqrsStatus status, PqrsType tipo);
}
