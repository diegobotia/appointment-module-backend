package com.ipscentir.appointments.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class N8nApiSecurityTest {

    private static final String N8N_API_KEY_HEADER = "X-API-Key";
    private static final String VALID_KEY = "test-n8n-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectN8nRequestWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/integrations/n8n/patient/registration-status")
                        .param("codTipoIdentificacion", "CC")
                        .param("numIdentificacion", "123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectN8nRequestWithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/integrations/n8n/patient/registration-status")
                        .header(N8N_API_KEY_HEADER, "invalid-key")
                        .param("codTipoIdentificacion", "CC")
                        .param("numIdentificacion", "123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowN8nRequestWithValidApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/integrations/n8n/patient/registration-status")
                        .header(N8N_API_KEY_HEADER, VALID_KEY)
                        .param("codTipoIdentificacion", "CC")
                        .param("numIdentificacion", "123"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectN8nIdentifyWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/n8n/patient/identify")
                        .contentType("application/json")
                        .content("{\"codTipoIdentificacion\":\"CC\",\"numIdentificacion\":\"123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
