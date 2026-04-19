package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainEventRepository {

    DomainEventRecord save(DomainEventRecord record);

    Optional<DomainEventRecord> findById(UUID id);

    List<DomainEventRecord> findByPublishedFalseOrderByOccurredOnAsc();
}