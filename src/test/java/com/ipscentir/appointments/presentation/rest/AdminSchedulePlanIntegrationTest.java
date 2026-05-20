package com.ipscentir.appointments.presentation.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanBlockRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanSlotRequest;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SchedulePlanJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSchedulePlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialistJpaRepository specialistJpaRepository;

    @Autowired
    private SchedulePlanJpaRepository schedulePlanJpaRepository;

    private String specialistId;

    @BeforeEach
    void setUp() {
        schedulePlanJpaRepository.deleteAll();
        specialistJpaRepository.deleteAll();

        Specialist specialist = specialistJpaRepository.save(Specialist.builder()
                .id(UUID.randomUUID().toString())
                .numeroMedico("12345")
                .firstName("Laura")
                .lastName("Quintero")
                .active(true)
                .build());

        specialistId = specialist.getId();
    }

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        CreateSchedulePlanRequest request = buildPlanRequest(2026, 2);

        mockMvc.perform(post("/api/v1/admin/schedule-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ESPECIALISTA")
    void shouldRejectEspecialistaRole() throws Exception {
        CreateSchedulePlanRequest request = buildPlanRequest(2026, 2);

        mockMvc.perform(post("/api/v1/admin/schedule-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleQuarterlyVersioningAndPublication() throws Exception {
        UUID planV1Id = createPlanAndExtractId(buildPlanRequest(2026, 2));

        mockMvc.perform(post("/api/v1/admin/schedule-plans/{planId}/publish", planV1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.activeVersion").value(true));

        UUID planV2Id = createPlanAndExtractId(buildPlanRequest(2026, 2));

        mockMvc.perform(post("/api/v1/admin/schedule-plans/{planId}/publish", planV2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.activeVersion").value(true));

        mockMvc.perform(get("/api/v1/admin/schedule-plans/{planId}", planV1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.activeVersion").value(false));

        mockMvc.perform(get("/api/v1/admin/schedule-plans/specialists/{specialistId}", specialistId)
                        .param("year", "2026")
                        .param("quarter", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNumber").value(2))
                .andExpect(jsonPath("$[0].activeVersion").value(true))
                .andExpect(jsonPath("$[1].versionNumber").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAddBlockRangeToPlan() throws Exception {
        UUID planId = createPlanAndExtractId(buildPlanRequest(2026, 3));

        CreateSchedulePlanBlockRequest blockRequest = new CreateSchedulePlanBlockRequest(
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 20),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                "Capacitacion institucional",
                null
        );

        mockMvc.perform(post("/api/v1/admin/schedule-plans/{planId}/blocks", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks.length()").value(1))
                .andExpect(jsonPath("$.blocks[0].reason").value("Capacitacion institucional"));
    }

    private CreateSchedulePlanRequest buildPlanRequest(int year, int quarter) {
        return new CreateSchedulePlanRequest(
                specialistId,
                year,
                quarter,
                List.of(
                        new CreateSchedulePlanSlotRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 1),
                        new CreateSchedulePlanSlotRequest(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(17, 0), 30, 1)
                )
        );
    }

    private UUID createPlanAndExtractId(CreateSchedulePlanRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/schedule-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
