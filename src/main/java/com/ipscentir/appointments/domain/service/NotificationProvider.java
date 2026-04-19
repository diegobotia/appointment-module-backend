package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.notification.Notification;

public interface NotificationProvider {
    
    boolean sendNotification(Notification notification);
}
