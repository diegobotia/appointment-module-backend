package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.MedicoEspecialidadRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.MedicoEspecialidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMedicoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpecialistJpaRepository specialistJpaRepository;

    @Autowired
    private MedicoEspecialidadRepository medicoEspecialidadRepository;

    private String targetMedicoId;
    private String inactiveMedicoId;

    @BeforeEach
    void setUp() {
        specialistJpaRepository.deleteAll();
        medicoEspecialidadRepository.deleteAll();

        for (int i = 1; i <= 25; i++) {
            String docId = UUID.randomUUID().toString();
            String specialty = i % 2 == 0 ? "Dolor" : "Psicologia";
            specialistJpaRepository.save(Specialist.builder()
                    .id(docId)
                    .numeroMedico("MED-" + String.format("%03d", i))
                    .tipoDoc("CC")
                    .numDoc(String.valueOf(10_000 + i))
                    .firstName(i == 3 ? "Ana" : "Medico" + i)
                    .lastName(i == 3 ? "Martinez" : "Apellido" + i)
                    .specialty(specialty)
                    .active(true)
                    .build());
            medicoEspecialidadRepository.save(MedicoEspecialidad.builder()
                    .id(UUID.randomUUID())
                    .medicoId(UUID.fromString(docId))
                    .especialidad(specialty)
                    .activo(true)
                    .build());
        }

        Specialist inactive = specialistJpaRepository.save(Specialist.builder()
                .id(UUID.randomUUID().toString())
                .numeroMedico("MED-INACTIVE")
                .tipoDoc("CC")
                .numDoc("99999")
                .firstName("Inactivo")
                .lastName("Medico")
                .specialty("Dolor")
                .active(false)
                .build());
        inactiveMedicoId = inactive.getId();

        targetMedicoId = specialistJpaRepository.findByNumeroMedico("MED-003")
                .orElseThrow()
                .getId();
    }

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void shouldRejectNonAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldPaginateActiveMedicos() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/admin/specialists")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(25));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldSearchByNamePartialMatch() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos").param("q", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana Martinez"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldSearchByNumDocExactMatch() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos").param("numDoc", "10003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].registro").value("MED-003"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldReturnMedicoDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos/{id}", targetMedicoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetMedicoId))
                .andExpect(jsonPath("$.registro").value("MED-003"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldReturnNotFoundForUnknownMedico() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldFilterBySpecialtyCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos").param("specialty", "dolor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(12)));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldIncludeInactiveWhenRequested() throws Exception {
        mockMvc.perform(get("/api/v1/admin/medicos").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(inactiveMedicoId))
                .andExpect(jsonPath("$.content[0].active").value(false));
    }
}
