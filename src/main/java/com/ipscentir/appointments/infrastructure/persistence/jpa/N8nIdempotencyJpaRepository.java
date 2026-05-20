package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.integration.N8nIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface N8nIdempotencyJpaRepository extends JpaRepository<N8nIdempotencyRecord, UUID> {

    Optional<N8nIdempotencyRecord> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);
}
