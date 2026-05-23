package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DomainEventJpaRepository extends JpaRepository<DomainEventRecord, UUID> {

    List<DomainEventRecord> findByPublishedFalseOrderByOccurredOnAsc();

    @Query("""
            SELECT e FROM DomainEventRecord e
            WHERE (:eventType IS NULL OR e.eventType = :eventType)
              AND (:published IS NULL OR e.published = :published)
            ORDER BY e.occurredOn DESC
            """)
    List<DomainEventRecord> search(
            @Param("eventType") String eventType,
            @Param("published") Boolean published,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(e) FROM DomainEventRecord e
            WHERE (:eventType IS NULL OR e.eventType = :eventType)
              AND (:published IS NULL OR e.published = :published)
            """)
    long countSearch(@Param("eventType") String eventType, @Param("published") Boolean published);
}
