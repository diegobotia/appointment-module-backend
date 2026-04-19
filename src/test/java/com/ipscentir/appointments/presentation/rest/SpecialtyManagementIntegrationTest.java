package com.ipscentir.appointments.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.specialist.AssignSpecialtyRequest;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.domain.model.specialty.Specialty;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialtyJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpecialtyManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialistJpaRepository specialistJpaRepository;

    @Autowired
    private SpecialtyJpaRepository specialtyJpaRepository;

    @BeforeEach
    void cleanUp() {
        specialistJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListActiveSpecialties() throws Exception {
        mockMvc.perform(get("/api/v1/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAssignUpToFourSpecialtiesAndRejectFifth() throws Exception {
        Specialist specialist = specialistJpaRepository.save(
                Specialist.builder()
                        .firstName("Ana")
                        .lastName("Lopez")
                        .email("ana.lopez@ips.test")
                        .active(true)
                        .build()
        );

        List<UUID> specialtyIds = specialtyJpaRepository.findAll().stream()
                .sorted(Comparator.comparing(Specialty::getCode))
                .map(Specialty::getId)
                .toList();

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/admin/specialists/{specialistId}/specialties", specialist.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssignSpecialtyRequest(specialtyIds.get(i)))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/admin/specialists/{specialistId}/specialties", specialist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignSpecialtyRequest(specialtyIds.get(4)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Specialist cannot have more than 4 specialties"));

        Specialist reloaded = specialistJpaRepository.findWithSpecialtiesById(specialist.getId()).orElseThrow();
        assertThat(reloaded.getSpecialties()).hasSize(4);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRemoveSpecialtyAndFilterInactiveSpecialistsByDefault() throws Exception {
        Specialist specialist = specialistJpaRepository.save(
                Specialist.builder()
                        .firstName("Luis")
                        .lastName("Perez")
                        .email("luis.perez@ips.test")
                        .active(true)
                        .build()
        );

        Specialty specialty = specialtyJpaRepository.findAll().stream()
                .sorted(Comparator.comparing(Specialty::getCode))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/api/v1/admin/specialists/{specialistId}/specialties", specialist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignSpecialtyRequest(specialty.getId()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/specialties/{specialtyId}/specialists", specialty.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("luis.perez@ips.test"));

        mockMvc.perform(patch("/api/v1/admin/specialists/{specialistId}/deactivate", specialist.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/specialties/{specialtyId}/specialists", specialty.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/v1/specialties/{specialtyId}/specialists", specialty.getId())
                        .param("activeOnly", "false")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "lastName,asc")
                        .param("sort", "firstName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(delete("/api/v1/admin/specialists/{specialistId}/specialties/{specialtyId}", specialist.getId(), specialty.getId()))
                .andExpect(status().isOk());

                Specialist reloaded = specialistJpaRepository.findWithSpecialtiesById(specialist.getId()).orElseThrow();
        assertThat(reloaded.getSpecialties()).isEmpty();
    }
}