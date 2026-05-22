package com.ipscentir.appointments.presentation.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentOperationsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectUnauthenticatedList() throws Exception {
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    void shouldAllowFacturacionReadOnly() throws Exception {
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    void shouldForbidFacturacionConfirm() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/{id}/confirm", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void shouldForbidMedicoCreate() throws Exception {
        String body = """
                {
                  "patientId": "00000000-0000-0000-0000-000000000099",
                  "doctorId": "00000000-0000-0000-0000-000000000001",
                  "sedeId": 2,
                  "scheduleId": "00000000-0000-0000-0000-000000000003",
                  "appointmentDate": "2099-12-01",
                  "appointmentTime": "10:00:00",
                  "appointmentType": "PRESENCIAL",
                  "reason": "Test"
                }
                """;
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldForbidAdmisionesTherapyCutoff() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/therapy/pending-group/process-cutoff"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ASESOR")
    void shouldAllowAsesorListAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ASESOR")
    void shouldForbidAsesorTherapyCutoff() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/therapy/pending-group/process-cutoff"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ASESOR")
    void shouldForbidAsesorAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/kpis"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ASESOR")
    void shouldAllowAsesorAdminAppointmentsSearch() throws Exception {
        mockMvc.perform(get("/api/v1/admin/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldAllowAdmisionesAdminAppointmentsSearch() throws Exception {
        mockMvc.perform(get("/api/v1/admin/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldAllowAdministracionTherapyCutoff() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/therapy/pending-group/process-cutoff"))
                .andExpect(status().isOk());
    }
}
