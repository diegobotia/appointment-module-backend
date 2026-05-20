package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.notification.Notification;
import com.ipscentir.appointments.domain.model.notification.NotificationPurpose;
import com.ipscentir.appointments.domain.model.notification.NotificationStatus;
import com.ipscentir.appointments.domain.model.notification.NotificationType;
import com.ipscentir.appointments.infrastructure.persistence.jpa.NotificationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @MockBean
    private com.ipscentir.appointments.domain.service.NotificationProvider notificationProvider;

    private UUID notificationId;

    @BeforeEach
    void setUp() {
        notificationJpaRepository.deleteAll();

        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.EMAIL,
                NotificationPurpose.REMINDER_24H,
                "patient@example.com",
                "Recordatorio de prueba"
        );
        notification.recordFailedAttempt("Simulated failure");
        notificationId = notificationJpaRepository.save(notification).getId();
    }

    @Test
    void shouldRejectWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldListFailedNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notifications")
                        .param("status", NotificationStatus.FAILED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.content[0].retryable").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldRetryFailedNotification() throws Exception {
        when(notificationProvider.sendNotification(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/admin/notifications/{id}/retry", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }
}
