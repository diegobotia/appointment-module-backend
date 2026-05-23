package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsType;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PqrsJpaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private PqrsJpaRepository pqrsJpaRepository;

    @BeforeEach
    void setUp() {
        appointmentJpaRepository.deleteAll();
        pqrsJpaRepository.deleteAll();

        LocalDate today = LocalDate.now();
        appointmentJpaRepository.save(Appointment.scheduleNew(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                null,
                new AppointmentScheduleData(
                        UUID.randomUUID(),
                        FacilityMasterData.SEDE_ID_BELEN,
                        today,
                        LocalTime.of(9, 0),
                        30,
                        AppointmentType.PRESENCIAL,
                        AppointmentStatus.CONFIRMED,
                        "Consulta"
                )
        ));

        pqrsJpaRepository.save(Pqrs.create(
                "123456789",
                PqrsType.PETICION,
                "Solicitud de información",
                "user@example.com",
                "Juan",
                "3001234567",
                "PQRS-2026-000001"
        ));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldReturnDashboardKpis() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentsToday").value(1))
                .andExpect(jsonPath("$.pqrsOpen").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldListAndUpdatePqrs() throws Exception {
        Pqrs pqrs = pqrsJpaRepository.findAll().getFirst();

        mockMvc.perform(get("/api/v1/admin/pqrs").param("status", PqrsStatus.CREADO.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(patch("/api/v1/admin/pqrs/{id}/status", pqrs.getId())
                        .contentType("application/json")
                        .content("{\"status\":\"EN_REVISION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_REVISION"));
    }
}
