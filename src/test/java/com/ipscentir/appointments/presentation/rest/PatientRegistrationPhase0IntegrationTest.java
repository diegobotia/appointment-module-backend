package com.ipscentir.appointments.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.form.CreatePatientRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientRegistrationPhase0IntegrationTest {

    private static final String N8N_API_KEY_HEADER = "X-API-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposePublicFormConfigWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/forms/patients/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formBaseUrl").exists())
                .andExpect(jsonPath("$.urlTemplate").exists())
                .andExpect(jsonPath("$.genders").exists())
                .andExpect(jsonPath("$.civilStatus").exists())
                .andExpect(jsonPath("$.occupations").exists())
                .andExpect(jsonPath("$.bloodGroups").exists())
                .andExpect(jsonPath("$.schoolingLevels").exists())
                .andExpect(jsonPath("$.countries").exists())
                .andExpect(jsonPath("$.supportedDocumentTypes[?(@.codigo=='13')].descripcion")
                        .value("Cédula de ciudadanía"))
                .andExpect(jsonPath("$.supportedDocumentTypes.length()").value(13));
    }

    @Test
    void shouldRejectDirectAppointmentBookingWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLegacyAuthRegisterEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowN8nToCheckRegistrationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/integrations/n8n/patient/registration-status")
                        .header(N8N_API_KEY_HEADER, "test-n8n-api-key")
                        .param("codTipoIdentificacion", "CC")
                        .param("numIdentificacion", "999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(false))
                .andExpect(jsonPath("$.formUrl").exists());
    }

    @Test
    void shouldRegisterPatientViaPublicForm() throws Exception {
        String uniqueDocument = String.valueOf(System.nanoTime()).substring(0, 10);
        String uniqueEmail = "nuevo.paciente." + uniqueDocument + "@example.com";
        CreatePatientRegistrationRequest request = CreatePatientRegistrationRequest.builder()
                .email(uniqueEmail)
                .nombres("Ana")
                .apellidos("Lopez")
                .numIdentificacion(uniqueDocument)
                .codTipoIdentificacion("13")
                .fechaNacimiento(LocalDate.of(1995, 3, 10))
                .idGenero(UUID.randomUUID().toString())
                .idEstadoCivil(UUID.randomUUID().toString())
                .idOcupacion(UUID.randomUUID().toString())
                .idGrupoSanguineo(UUID.randomUUID().toString())
                .idEscolaridad(UUID.randomUUID().toString())
                .estrato(2)
                .idPaisOrigen(1L)
                .telefono("+57300" + uniqueDocument.substring(0, 8))
                .direccionDetalle("Calle 20 # 10-15")
                .codMunicipio("050001")
                .codZonaTerritorial("ZONA_1")
                .build();

        mockMvc.perform(post("/api/v1/forms/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").exists());

        mockMvc.perform(get("/api/v1/forms/patients/status")
                        .param("codTipoIdentificacion", "CC")
                        .param("numIdentificacion", uniqueDocument))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.nombres").value("Ana"));
    }
}
