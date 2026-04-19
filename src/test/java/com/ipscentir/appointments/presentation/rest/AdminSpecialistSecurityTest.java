package com.ipscentir.appointments.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.specialist.CreateSpecialistRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSpecialistSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
        CreateSpecialistRequest request = new CreateSpecialistRequest("Ana", "Lopez", "ana.lopez@ips.test");

        mockMvc.perform(post("/api/v1/admin/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "especialista", roles = {"ESPECIALISTA"})
    void shouldReturnForbiddenForEspecialistaRole() throws Exception {
        CreateSpecialistRequest request = new CreateSpecialistRequest("Ana", "Lopez", "ana.lopez@ips.test");

        mockMvc.perform(post("/api/v1/admin/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldAllowAdminToCreateSpecialist() throws Exception {
        CreateSpecialistRequest request = new CreateSpecialistRequest("Ana", "Lopez", "ana.lopez@ips.test");

        mockMvc.perform(post("/api/v1/admin/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
