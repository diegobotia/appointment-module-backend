package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import com.ipscentir.appointments.domain.repository.DomainEventRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DomainEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DomainEventRepositoryImpl implements DomainEventRepository {

    private final DomainEventJpaRepository jpaRepository;

    @Override
    public DomainEventRecord save(DomainEventRecord record) {
        return jpaRepository.save(record);
    }

    @Override
    public Optional<DomainEventRecord> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<DomainEventRecord> findByPublishedFalseOrderByOccurredOnAsc() {
        return jpaRepository.findByPublishedFalseOrderByOccurredOnAsc();
    }
}