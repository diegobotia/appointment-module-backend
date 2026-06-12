package com.ipscentir.appointments.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentResourceAllocationJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdministrativeAppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private AppointmentResourceAllocationJpaRepository allocationJpaRepository;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    private String doctorA;
    private String doctorB;
    private Integer sedeId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    @BeforeEach
    void setUp() {
        allocationJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();

        doctorA = UUID.randomUUID().toString();
        doctorB = UUID.randomUUID().toString();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        appointmentDate = LocalDate.now().plusDays(5);
        appointmentTime = LocalTime.of(11, 0);

        seedSchedule(doctorA);
        seedSchedule(doctorB);
    }

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/administrative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    void shouldRejectFacturacionRole() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/administrative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldCreateAdministrativeAppointmentWithoutPatient() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/administrative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(nullValue()))
                .andExpect(jsonPath("$.appointmentType").value("STAFF"))
                .andExpect(jsonPath("$.medicoId").value(doctorA))
                .andExpect(jsonPath("$.additionalMedicoIds[0]").value(doctorB))
                .andExpect(jsonPath("$.sedeId").value(sedeId))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.administrative").value(true))
                .andExpect(jsonPath("$.patientDisplayName").value(nullValue()));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldRejectDuplicateParticipants() throws Exception {
        Map<String, Object> payload = Map.of(
                "participantDoctorIds", List.of(doctorA, doctorA),
                "sedeId", sedeId,
                "appointmentDate", appointmentDate,
                "appointmentTime", appointmentTime,
                "durationMinutes", 30,
                "reason", "Duplicado"
        );

        mockMvc.perform(post("/api/v1/appointments/administrative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldRejectWhenStaffRoomAlreadyOccupied() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/administrative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated());

        String otherDoctor = UUID.randomUUID().toString();
        seedSchedule(otherDoctor);

        mockMvc.perform(post("/api/v1/appointments/administrative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "participantDoctorIds", List.of(otherDoctor),
                                "sedeId", sedeId,
                                "appointmentDate", appointmentDate,
                                "appointmentTime", appointmentTime,
                                "durationMinutes", 30,
                                "reason", "Conflicto sala"
                        ))))
                .andExpect(status().isConflict());
    }

    private Map<String, Object> validPayload() {
        return Map.of(
                "participantDoctorIds", List.of(doctorA, doctorB),
                "sedeId", sedeId,
                "appointmentDate", appointmentDate,
                "appointmentTime", appointmentTime,
                "durationMinutes", 60,
                "reason", "Junta operativa"
        );
    }

    private void seedSchedule(String doctorId) {
        scheduleJpaRepository.save(Schedule.builder()
                .doctorId(doctorId)
                .sedeId(sedeId)
                .specialty(AppointmentServiceTypeLabel.STAFF)
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build());
    }

    /** Evita dependencia de application en tests de presentación. */
    private static final class AppointmentServiceTypeLabel {
        static final String STAFF = "Staff";
    }
}
