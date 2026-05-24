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
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[?(@.code == 'STAFF')].displayName").value("Staff"));
    }

    @Test
    void shouldReturnVersionedSpecialtyCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/catalogs/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].code").value("FISIATRIA"))
                .andExpect(jsonPath("$[0].displayName").value("Fisiatria"))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[1].code").value("FISIATRIA_INTEGRAL_DOLOR"))
                .andExpect(jsonPath("$[1].displayName").value("Fisiatria integral del dolor"))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[2].code").value("DOLOR"))
                .andExpect(jsonPath("$[2].displayName").value("Dolor"))
                .andExpect(jsonPath("$[2].version").value(1));
    }

    @Test
    void openApiShouldDocumentEnrichedAppointmentAndAdministrativeEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("1.1"))
                .andExpect(jsonPath("$.components.schemas.AppointmentDTO.properties.medicoDisplayName").exists())
                .andExpect(jsonPath("$.components.schemas.AppointmentDTO.properties.patientDisplayName").exists())
                .andExpect(jsonPath("$.components.schemas.AppointmentDTO.properties.administrative").exists())
                .andExpect(jsonPath("$.components.schemas.CreateAdministrativeAppointmentCommand").exists())
                .andExpect(jsonPath("$.paths['/api/v1/appointments/administrative'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/staff/patients/search'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/medicos'].get").exists())
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt']").exists());
    }
}
