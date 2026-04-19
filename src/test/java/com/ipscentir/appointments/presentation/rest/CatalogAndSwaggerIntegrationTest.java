package com.ipscentir.appointments.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogAndSwaggerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnVersionedAppointmentServiceCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/catalogs/appointment-service-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[0].version").value(1));
    }

    @Test
    void shouldReturnVersionedSpecialtyCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/catalogs/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].version").value(1));
    }
}
