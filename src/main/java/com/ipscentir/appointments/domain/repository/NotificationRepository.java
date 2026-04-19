package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.notification.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    
    Notification save(Notification notification);
    
    Optional<Notification> findById(UUID id);
    
    List<Notification> findByExternalEntityId(UUID externalEntityId);
}
