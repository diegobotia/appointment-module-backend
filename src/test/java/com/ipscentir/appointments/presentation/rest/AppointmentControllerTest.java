package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.service.AppointmentOperationsService;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentOperationsService appointmentOperationsService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void testBookAppointment_Success() throws Exception {
        String doctorId = UUID.randomUUID().toString();
        UUID patientId = UUID.randomUUID();
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        UUID scheduleId = UUID.randomUUID();

        CreateAppointmentCommand command = new CreateAppointmentCommand(
                patientId, doctorId, sedeId, null, scheduleId, LocalDate.now().plusDays(2), LocalTime.of(10, 0), "PRESENCIAL", "Routine Visit", null, null
        );

        AppointmentDTO dto = new AppointmentDTO(
                UUID.randomUUID(), patientId, doctorId, sedeId, null, scheduleId, LocalDate.now().plusDays(2), LocalTime.of(10, 0),
                30, AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, BookingChannel.STAFF, null, "Routine Visit", null, LocalDateTime.now(), null
        );

        Mockito.when(appointmentOperationsService.createAppointment(any(CreateAppointmentCommand.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.appointmentType").value("PRESENCIAL"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void testBookAppointment_FailsDueToValidationErrors() throws Exception {
        // Missing patientId and doctorId to trigger 400 Bad Request
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                null, null, null, null, null, LocalDate.now().plusDays(2), LocalTime.of(10, 0), "PRESENCIAL", "Routine Visit", null, null
        );

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.patientId").exists())
                .andExpect(jsonPath("$.doctorId").exists());
    }
}
