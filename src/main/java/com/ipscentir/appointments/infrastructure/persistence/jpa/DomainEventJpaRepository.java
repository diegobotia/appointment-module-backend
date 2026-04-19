package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DomainEventJpaRepository extends JpaRepository<DomainEventRecord, UUID> {

    List<DomainEventRecord> findByPublishedFalseOrderByOccurredOnAsc();
}