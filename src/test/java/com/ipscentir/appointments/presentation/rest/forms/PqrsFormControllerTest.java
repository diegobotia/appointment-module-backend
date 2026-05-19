package com.ipscentir.appointments.presentation.rest.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.form.CreatePqrsRequest;
import com.ipscentir.appointments.application.dto.form.PqrsResponse;
import com.ipscentir.appointments.application.service.PqrsApplicationService;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("null")
class PqrsFormControllerTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PqrsApplicationService pqrsApplicationService;

    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void shouldSubmitPqrsWithValidData() throws Exception {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "1234567890",
                "QUEJA",
                "Esta es una queja detallada sobre el servicio recibido",
                "user@example.com",
                "Juan Pérez",
                "3001234567"
        );

        PqrsResponse response = new PqrsResponse(
                "PQRS-2026-000001",
                "CREADO",
                "Su PQRS ha sido registrada correctamente",
                LocalDateTime.now(),
                "1234567890"
        );

        when(pqrsApplicationService.createPqrs(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/forms/pqrs")
                .contentType(JSON)
                .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.radicado").value("PQRS-2026-000001"))
                .andExpect(jsonPath("$.status").value("CREADO"))
                .andExpect(jsonPath("$.cedula").value("1234567890"))
                .andExpect(jsonPath("$.message").value("Su PQRS ha sido registrada correctamente"));
    }

    @Test
    void shouldRejectEmptyCedula() throws Exception {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "",
                "QUEJA",
                "Esta es una queja detallada sobre el servicio recibido",
                "user@example.com",
                "Juan Pérez",
                "3001234567"
        );

        mockMvc.perform(post("/api/v1/forms/pqrs")
                .contentType(JSON)
                .content(asJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidCedula() throws Exception {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "111111",
                "QUEJA",
                "Esta es una queja detallada sobre el servicio recibido",
                "user@example.com",
                "Juan Pérez",
                "3001234567"
        );

        mockMvc.perform(post("/api/v1/forms/pqrs")
                .contentType(JSON)
                .content(asJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidEmailFormat() throws Exception {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "1234567890",
                "QUEJA",
                "Esta es una queja detallada sobre el servicio recibido",
                "invalid-email",
                "Juan Pérez",
                "3001234567"
        );

        mockMvc.perform(post("/api/v1/forms/pqrs")
                .contentType(JSON)
                .content(asJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidPqrsType() throws Exception {
        Map<String, Object> request = Map.of(
                "cedula", "1234567890",
                "tipo", "INVALID_TYPE",
                "descripcion", "Esta es una queja detallada sobre el servicio recibido",
                "correo", "user@example.com",
                "nombres", "Juan Pérez",
                "telefono", "3001234567"
        );

        mockMvc.perform(post("/api/v1/forms/pqrs")
                .contentType(JSON)
                .content(asJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectShortDescription() throws Exception {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "1234567890",
                "QUEJA",
                "Corta",
                "user@example.com",
                "Juan Pérez",
                "3001234567"
        );

        mockMvc.perform(post("/api/v1/forms/pqrs")
                .contentType(JSON)
                .content(asJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptOptionalFieldsEmpty() throws Exception {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "1234567890",
                "PETICION",
                "Esta es una petición detallada sobre lo que necesito",
                "user@example.com",
                null,
                null
        );

        PqrsResponse response = new PqrsResponse(
                "PQRS-2026-000002",
                "CREADO",
                "Su PQRS ha sido registrada correctamente",
                LocalDateTime.now(),
                "1234567890"
        );

        when(pqrsApplicationService.createPqrs(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/forms/pqrs")
                .contentType(JSON)
                .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.radicado").value("PQRS-2026-000002"));
    }

    @Test
    void shouldAcceptAllPqrsTypes() throws Exception {
        String[] types = {"PETICION", "QUEJA", "RECLAMO", "SUGERENCIA"};

        for (String type : types) {
            CreatePqrsRequest request = new CreatePqrsRequest(
                    "1234567890",
                    type,
                    "Esta es una " + type.toLowerCase() + " detallada",
                    "user@example.com",
                    "Juan Pérez",
                    "3001234567"
            );

            PqrsResponse response = new PqrsResponse(
                    "PQRS-2026-000003",
                    "CREADO",
                    "Su PQRS ha sido registrada correctamente",
                    LocalDateTime.now(),
                    "1234567890"
            );

            when(pqrsApplicationService.createPqrs(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/forms/pqrs")
                    .contentType(JSON)
                    .content(asJson(request)))
                    .andExpect(status().isCreated());
        }
    }
}
