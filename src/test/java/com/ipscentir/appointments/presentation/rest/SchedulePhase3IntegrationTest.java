package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.FacilityJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchedulePhase3IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpecialistJpaRepository specialistJpaRepository;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private FacilityJpaRepository facilityJpaRepository;

    private String medicoProfileId;
    private UUID facilityId;
    private LocalDate monday;

    @BeforeEach
    void setUp() {
        scheduleJpaRepository.deleteAll();
        specialistJpaRepository.deleteAll();

        medicoProfileId = UUID.randomUUID().toString();

        specialistJpaRepository.save(Specialist.builder()
                .id(medicoProfileId)
                .numeroMedico("99001")
                .firstName("Ana")
                .lastName("Rios")
                .specialty("Medicina general")
                .active(true)
                .build());

        facilityId = facilityJpaRepository.findByCode("SEDE_NORTE").orElseThrow().getId();
        monday = LocalDate.now().plusDays(1);
        while (monday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }

        scheduleJpaRepository.save(Schedule.builder()
                .doctorId(medicoProfileId)
                .facilityId(facilityId)
                .specialty("Medicina general")
                .dayOfWeek(java.time.DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldReturnDoctorAvailabilityInRange() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/{doctorId}/availability", medicoProfileId)
                        .param("facilityId", facilityId.toString())
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(medicoProfileId))
                .andExpect(jsonPath("$.totalAvailableSlots").value(4))
                .andExpect(jsonPath("$.days[0].slots.length()").value(4));
    }

    @Test
    void medicoCannotViewOtherDoctorAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/{doctorId}/availability", medicoProfileId)
                        .with(user(UUID.randomUUID().toString()).roles("MEDICO"))
                        .param("facilityId", facilityId.toString())
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicoCanViewOwnAvailabilityAndMeSchedule() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/{doctorId}/availability", medicoProfileId)
                        .with(user(medicoProfileId).roles("MEDICO"))
                        .param("facilityId", facilityId.toString())
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailableSlots").value(4));

        mockMvc.perform(get("/api/v1/me/schedule")
                        .with(user(medicoProfileId).roles("MEDICO"))
                        .param("facilityId", facilityId.toString())
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(medicoProfileId))
                .andExpect(jsonPath("$.scheduleTemplates.length()").value(1));
    }
}
