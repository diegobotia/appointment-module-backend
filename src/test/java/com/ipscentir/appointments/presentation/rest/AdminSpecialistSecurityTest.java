package com.ipscentir.appointments.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSpecialistSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/admin/specialists"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "medico", roles = {"MEDICO"})
    void shouldReturnForbiddenForMedicoRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/specialists"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "administracion", roles = {"ADMINISTRACION"})
    void shouldAllowAdministracionToAccessSpecialists() throws Exception {
        mockMvc.perform(get("/api/v1/admin/specialists"))
                .andExpect(status().isOk());
    }
}
