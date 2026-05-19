package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import com.ipscentir.appointments.domain.repository.PqrsRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PqrsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PqrsRepositoryImpl implements PqrsRepository {

    private final PqrsJpaRepository jpaRepository;

    @Override
    public Pqrs save(Pqrs pqrs) {
        return jpaRepository.save(pqrs);
    }

    @Override
    public Optional<Pqrs> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Pqrs> findByRadicado(String radicado) {
        return jpaRepository.findByRadicado(radicado);
    }

    @Override
    public long countByYearCreated(int year) {
        return jpaRepository.countByYearCreated(year);
    }
}
