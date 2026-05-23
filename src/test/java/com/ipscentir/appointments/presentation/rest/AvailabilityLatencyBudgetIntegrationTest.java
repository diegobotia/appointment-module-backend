package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("null")
class AvailabilityLatencyBudgetIntegrationTest {

    private static final String N8N_API_KEY_HEADER = "X-API-Key";
    private static final String N8N_API_KEY = "test-n8n-api-key";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final long LATENCY_BUDGET_MS = 500;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private SedeJpaRepository sedeJpaRepository;

    private String doctorId;
    private Integer sedeId;
    private LocalDate queryDate;

    @BeforeEach
    void setUp() {
        appointmentJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();

        doctorId = UUID.randomUUID().toString();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        queryDate = LocalDate.now().plusDays(2);

        scheduleJpaRepository.save(Schedule.builder()
            .doctorId(doctorId.toString())
                .sedeId(sedeId)
                .specialty("Terapia fisica")
                .dayOfWeek(queryDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(5)
                .isActive(true)
                .build());
    }

    @Test
    void availabilityEndpointShouldMeetLatencyBudget() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "doctorId", doctorId,
                "facilityId", "BELEN",
                "serviceType", "TERAPIA_FISICA",
                "date", queryDate));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/integrations/n8n/patient/availability")
                    .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                    .contentType(JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());
        }

        List<Long> latenciesMs = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            long startNanos = System.nanoTime();
            mockMvc.perform(post("/api/v1/integrations/n8n/patient/availability")
                    .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                    .contentType(JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            latenciesMs.add(elapsedMs);
        }

        latenciesMs.sort(Comparator.naturalOrder());
        int p95Index = (int) Math.ceil(0.95 * latenciesMs.size()) - 1;
        long p95Ms = latenciesMs.get(p95Index);

        assertTrue(
                p95Ms < LATENCY_BUDGET_MS,
                "Expected p95 latency under " + LATENCY_BUDGET_MS + "ms but got " + p95Ms + "ms");
    }
}
