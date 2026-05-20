package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByEntityId(UUID entityId);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.status = com.ipscentir.appointments.domain.model.notification.NotificationStatus.FAILED
              AND n.retryCount < :maxAttempts
            ORDER BY n.createdAt ASC
            """)
    List<Notification> findRetryable(@Param("maxAttempts") int maxAttempts, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT n FROM Notification n
            WHERE (:status IS NULL OR n.status = :status)
              AND (:type IS NULL OR n.notificationType = :type)
              AND (:entityId IS NULL OR n.entityId = :entityId)
            ORDER BY n.createdAt DESC
            """)
    List<Notification> search(
            @Param("status") NotificationStatus status,
            @Param("type") NotificationType type,
            @Param("entityId") UUID entityId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE (:status IS NULL OR n.status = :status)
              AND (:type IS NULL OR n.notificationType = :type)
              AND (:entityId IS NULL OR n.entityId = :entityId)
            """)
    long countSearch(
            @Param("status") NotificationStatus status,
            @Param("type") NotificationType type,
            @Param("entityId") UUID entityId
    );

    long countByStatus(NotificationStatus status);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.status = com.ipscentir.appointments.domain.model.notification.NotificationStatus.FAILED
              AND n.retryCount < :maxAttempts
            """)
    long countRetryable(@Param("maxAttempts") int maxAttempts);
}
