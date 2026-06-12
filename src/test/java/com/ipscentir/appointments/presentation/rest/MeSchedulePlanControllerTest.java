package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.schedule.SchedulePlanBlockDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanSlotDTO;
import com.ipscentir.appointments.application.security.StaffSecurityHelper;
import com.ipscentir.appointments.application.service.SchedulePlanAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeSchedulePlanControllerTest {

    private static final String MEDICO_ID = "medico-123";
    private static final String MEDICO_ID_2 = "medico-456";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaffSecurityHelper staffSecurityHelper;

    @MockBean
    private SchedulePlanAdminService schedulePlanAdminService;

    @Test
    @WithMockUser(roles = "MEDICO")
    void getMySchedulePlans_ShouldReturn200_WhenMedicoHasPlans() throws Exception {
        SchedulePlanDTO plan = createSamplePlan(MEDICO_ID, 1);
        when(staffSecurityHelper.requireDoctorIdForMedico()).thenReturn(MEDICO_ID);
        when(schedulePlanAdminService.listByMedico(eq(MEDICO_ID), isNull(), isNull()))
                .thenReturn(List.of(plan));

        mockMvc.perform(get("/api/v1/me/schedule-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].specialistId").value(MEDICO_ID))
                .andExpect(jsonPath("$[0].versionNumber").value(1));
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void getMySchedulePlans_ShouldReturn200_WhenMedicoHasNoPlans() throws Exception {
        when(staffSecurityHelper.requireDoctorIdForMedico()).thenReturn(MEDICO_ID_2);
        when(schedulePlanAdminService.listByMedico(eq(MEDICO_ID_2), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/me/schedule-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void getMySchedulePlans_ShouldResolveDoctorIdFromSecurityContext() throws Exception {
        SchedulePlanDTO plan = createSamplePlan(MEDICO_ID, 2);
        when(staffSecurityHelper.requireDoctorIdForMedico()).thenReturn(MEDICO_ID);
        when(schedulePlanAdminService.listByMedico(eq(MEDICO_ID), isNull(), isNull()))
                .thenReturn(List.of(plan));

        mockMvc.perform(get("/api/v1/me/schedule-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(2));
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void getMySchedulePlans_ShouldReturnPlansFilteredByDateRange() throws Exception {
        SchedulePlanDTO plan = createSamplePlan(MEDICO_ID, 1);
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        when(staffSecurityHelper.requireDoctorIdForMedico()).thenReturn(MEDICO_ID);
        when(schedulePlanAdminService.listByMedico(eq(MEDICO_ID), eq(start), eq(end)))
                .thenReturn(List.of(plan));

        mockMvc.perform(get("/api/v1/me/schedule-plans")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getMySchedulePlans_ShouldReturn403_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/me/schedule-plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void getMySchedulePlans_ShouldReturn403_WhenNotMedicoRole() throws Exception {
        mockMvc.perform(get("/api/v1/me/schedule-plans"))
                .andExpect(status().isForbidden());
    }

    private static SchedulePlanDTO createSamplePlan(String specialistId, int version) {
        return new SchedulePlanDTO(
                UUID.randomUUID(),
                specialistId,
                1,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                version,
                true,
                true,
                null,
                List.of(new SchedulePlanSlotDTO(
                        UUID.randomUUID(),
                        DayOfWeek.MONDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        30,
                        4,
                        UUID.randomUUID(),
                        true
                )),
                List.of()
        );
    }
}
