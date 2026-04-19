package com.ipscentir.appointments.presentation.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DomainEventJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("null")
class CriticalE2EFlowsIntegrationTest {

    private static final String N8N_API_KEY_HEADER = "X-API-Key";
    private static final String N8N_API_KEY = "test-n8n-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private DomainEventJpaRepository domainEventJpaRepository;

    @Autowired
    private SpecialistJpaRepository specialistJpaRepository;

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @BeforeEach
    void setUp() {
        appointmentJpaRepository.deleteAll();
        domainEventJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();
        specialistJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void e2eAdminCanCreateSpecialist() throws Exception {
        mockMvc.perform(post("/api/v1/admin/specialists")
                        .contentType(JSON)
                        .content(asJson(Map.of(
                                "firstName", "Carla",
                                "lastName", "Rojas",
                                "email", "carla.rojas@ips.test"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("carla.rojas@ips.test"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = "especialista", roles = {"ESPECIALISTA"})
    void e2eEspecialistaCannotCreateSpecialist() throws Exception {
        mockMvc.perform(post("/api/v1/admin/specialists")
                        .contentType(JSON)
                        .content(asJson(Map.of(
                                "firstName", "Luis",
                                "lastName", "Perez",
                                "email", "luis.perez@ips.test"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void e2eN8nPatientHappyFlowWithCancellationAndEventJournal() throws Exception {
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(9, 0);

        Schedule schedule = scheduleJpaRepository.save(Schedule.builder()
                .doctorId(doctorId)
                .facilityId(UUID.randomUUID())
                .specialty("GENERAL")
                .dayOfWeek(date.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(5)
                .isActive(true)
                .build());

        mockMvc.perform(post("/api/v1/integrations/n8n/patient/availability")
                .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(asJson(Map.of(
                                "doctorId", doctorId,
                                "date", date
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlotsCount").value(org.hamcrest.Matchers.greaterThan(0)));

        MvcResult createdResult = mockMvc.perform(post("/api/v1/integrations/n8n/patient/appointments")
                .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(asJson(Map.of(
                                "patientId", UUID.randomUUID(),
                                "doctorId", doctorId,
                                "scheduleId", schedule.getId(),
                                "appointmentDate", date,
                                "appointmentTime", time,
                                "appointmentType", "PRESENCIAL",
                                "reason", "Control general"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointment.status").value("SCHEDULED"))
                .andReturn();

        UUID appointmentId = UUID.fromString(readField(createdResult, "appointment.id"));

        mockMvc.perform(post("/api/v1/integrations/n8n/patient/appointments/{appointmentId}/cancel", appointmentId)
                .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(asJson(Map.of("reason", "Paciente solicita cambio"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointment.status").value("CANCELLED"));

        waitUntilEventJournalHas("APPOINTMENT_CREATED");
    }

    private void waitUntilEventJournalHas(String eventType) {
        for (int i = 0; i < 20; i++) {
            List<DomainEventRecord> events = domainEventJpaRepository.findAll();
            boolean found = events.stream().anyMatch(e -> eventType.equals(e.getEventType()));
            if (found) {
                return;
            }
            LockSupport.parkNanos(100_000_000L);
        }

        List<DomainEventRecord> events = domainEventJpaRepository.findAll();
        assertThat(events)
                .as("Expected event type %s in event journal", eventType)
                .anyMatch(e -> eventType.equals(e.getEventType()));
    }

    private String readField(MvcResult result, String dotPath) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String[] path = dotPath.split("\\.");
        JsonNode current = root;
        for (String part : path) {
            current = current.get(part);
        }
        return current.asText();
    }

    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}