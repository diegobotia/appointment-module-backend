package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.RoleJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.support.MedicoProfileTestSupport;
import com.ipscentir.appointments.support.MedicoProfileTestSupport.MedicoTestIdentity;
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
    private ProfileRepository profileRepository;

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    private String medicoId;
    private UUID medicoProfileId;
    private Integer sedeId;
    private LocalDate monday;

    @BeforeEach
    void setUp() {
        scheduleJpaRepository.deleteAll();
        profileRepository.deleteAll();
        specialistJpaRepository.deleteAll();

        MedicoTestIdentity identity = MedicoProfileTestSupport.seedMedicoWithProfile(
                specialistJpaRepository,
                profileRepository,
                roleJpaRepository,
                "99001",
                "Ana",
                "Rios"
        );
        medicoId = identity.medicoId();
        medicoProfileId = identity.profileId();

        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        monday = LocalDate.now().plusDays(1);
        while (monday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }

        scheduleJpaRepository.save(Schedule.builder()
                .doctorId(medicoId)
                .sedeId(sedeId)
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
        mockMvc.perform(get("/api/v1/medicos/{medicoId}/availability", medicoId)
                        .param("sedeId", sedeId.toString())
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicoId").value(medicoId))
                .andExpect(jsonPath("$.totalAvailableSlots").value(4))
                .andExpect(jsonPath("$.days[0].slots.length()").value(4));
    }

    @Test
    void medicoCannotViewOtherDoctorAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/medicos/{medicoId}/availability", medicoId)
                        .with(user(UUID.randomUUID().toString()).roles("MEDICO"))
                        .param("sedeId", sedeId.toString())
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicoCanViewOwnAvailabilityAndMeSchedule() throws Exception {
        mockMvc.perform(get("/api/v1/medicos/{medicoId}/availability", medicoId)
                        .with(user(medicoProfileId.toString()).roles("MEDICO"))
                        .param("sedeId", sedeId.toString())
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailableSlots").value(4));

        mockMvc.perform(get("/api/v1/me/schedule")
                        .with(user(medicoProfileId.toString()).roles("MEDICO"))
                        .param("sedeId", sedeId.toString())
                        .param("from", monday.toString())
                        .param("to", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicoId").value(medicoId))
                .andExpect(jsonPath("$.scheduleTemplates.length()").value(1));
    }
}
