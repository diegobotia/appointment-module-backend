package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanBlockRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanSlotRequest;
import com.ipscentir.appointments.application.dto.schedule.PublishSchedulePlanRequest;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
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

import static org.hamcrest.Matchers.containsString;
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

    @Autowired
    private SedeJpaRepository sedeJpaRepository;

    private String specialistId;
    private Integer sedeId;

    @BeforeEach
    void setUp() {
        schedulePlanJpaRepository.deleteAll();
        specialistJpaRepository.deleteAll();

        Specialist specialist = specialistJpaRepository.save(Specialist.builder()
                .id(UUID.randomUUID().toString())
                .numeroMedico("12345")
                .firstName("Laura")
                .lastName("Quintero")
                
                .build());

        specialistId = specialist.getId();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
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
    @WithMockUser(roles = "MEDICO")
    void shouldRejectMedicoRole() throws Exception {
        CreateSchedulePlanRequest request = buildPlanRequest(2026, 2);

        mockMvc.perform(post("/api/v1/admin/schedule-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldHandleQuarterlyVersioningAndPublication() throws Exception {
        UUID planV1Id = createPlanAndExtractId(buildPlanRequest(2026, 2));

        mockMvc.perform(post("/api/v1/admin/schedule-plans/{planId}/publish", planV1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PublishSchedulePlanRequest(sedeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.activeVersion").value(true));

        UUID planV2Id = createPlanAndExtractId(buildPlanRequest(2026, 2));

        mockMvc.perform(post("/api/v1/admin/schedule-plans/{planId}/publish", planV2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PublishSchedulePlanRequest(sedeId))))
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
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldRejectSlotBeforeFacilityOpening() throws Exception {
        CreateSchedulePlanRequest request = new CreateSchedulePlanRequest(
                specialistId,
                2026,
                2,
                List.of(new CreateSchedulePlanSlotRequest(
                        DayOfWeek.MONDAY,
                        LocalTime.of(6, 0),
                        LocalTime.of(10, 0),
                        30,
                        1
                ))
        );

        mockMvc.perform(post("/api/v1/admin/schedule-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.allowedWindow.openTime").value("07:00:00"))
                .andExpect(jsonPath("$.message", containsString("06:00")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldRejectSaturdayAfternoonSlot() throws Exception {
        CreateSchedulePlanRequest request = new CreateSchedulePlanRequest(
                specialistId,
                2026,
                2,
                List.of(new CreateSchedulePlanSlotRequest(
                        DayOfWeek.SATURDAY,
                        LocalTime.of(13, 0),
                        LocalTime.of(17, 0),
                        30,
                        1
                ))
        );

        mockMvc.perform(post("/api/v1/admin/schedule-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.allowedWindow.closeTime").value("12:00:00"))
                .andExpect(jsonPath("$.message", containsString("sábado")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldAcceptValidSlotAndPublish() throws Exception {
        CreateSchedulePlanRequest request = new CreateSchedulePlanRequest(
                specialistId,
                2026,
                2,
                List.of(new CreateSchedulePlanSlotRequest(
                        DayOfWeek.TUESDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        30,
                        1
                ))
        );

        UUID planId = createPlanAndExtractId(request);

        mockMvc.perform(post("/api/v1/admin/schedule-plans/{planId}/publish", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PublishSchedulePlanRequest(sedeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldRejectPublishWhenSlotViolatesFacilityHours() throws Exception {
        CreateSchedulePlanRequest request = new CreateSchedulePlanRequest(
                specialistId,
                2026,
                3,
                List.of(
                        new CreateSchedulePlanSlotRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 1),
                        new CreateSchedulePlanSlotRequest(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 1)
                )
        );

        UUID planId = createPlanAndExtractId(request);

        mockMvc.perform(post("/api/v1/admin/schedule-plans/{planId}/blocks", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSchedulePlanBlockRequest(
                                LocalDate.of(2026, 7, 11),
                                LocalDate.of(2026, 7, 11),
                                LocalTime.of(13, 0),
                                LocalTime.of(15, 0),
                                "Bloqueo sábado tarde",
                                null
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/schedule-plans/{planId}/publish", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PublishSchedulePlanRequest(sedeId))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.sedeId").value(sedeId.toString()))
                .andExpect(jsonPath("$.allowedWindow").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
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
