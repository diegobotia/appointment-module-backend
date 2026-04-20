package com.ipscentir.appointments.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nAvailabilityBookingPayloadDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nAvailabilitySlotDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventResponse;
import com.ipscentir.appointments.application.service.N8nPatientIntegrationService;
import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.AvailableSlotDTO;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("null")
class N8nIntegrationControllerTest {

        private static final MediaType JSON = MediaType.APPLICATION_JSON;
        private static final String N8N_API_KEY_HEADER = "X-API-Key";
        private static final String N8N_API_KEY = "test-n8n-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private N8nPatientIntegrationService n8nPatientIntegrationService;

    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void shouldRejectAvailabilityWithoutApiKey() throws Exception {
        UUID facilityId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);

                String body = asJson(Map.of("serviceType", "TERAPIA_FISICA", "facilityId", facilityId, "fromDate", date, "limit", 4));

        mockMvc.perform(post("/api/v1/integrations/n8n/patient/availability")
                        .contentType(JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldExposeAvailabilityWithApiKey() throws Exception {
        UUID facilityId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);

        when(n8nPatientIntegrationService.getAvailability(any())).thenReturn(new N8nPatientAvailabilityResponse(
                facilityId,
                "TERAPIA_FISICA",
                "Terapia fisica",
                date,
                4,
                1,
                List.of(new N8nAvailabilitySlotDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        facilityId,
                        "TERAPIA_FISICA",
                        "Terapia fisica",
                        date,
                        LocalTime.of(8, 0),
                        30,
                        "PRESENCIAL",
                        3,
                        new N8nAvailabilityBookingPayloadDTO(
                                null,
                                UUID.randomUUID(),
                                facilityId,
                                null,
                                UUID.randomUUID(),
                                date,
                                LocalTime.of(8, 0),
                                "PRESENCIAL",
                                "TERAPIA_FISICA",
                                "Terapia fisica",
                                30,
                                3,
                                null
                        )
                )),
                "Se encontraron 1 horarios disponibles para Terapia fisica."
        ));

        String body = asJson(Map.of("serviceType", "TERAPIA_FISICA", "facilityId", facilityId, "fromDate", date, "limit", 4));

        mockMvc.perform(post("/api/v1/integrations/n8n/patient/availability")
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facilityId").value(facilityId.toString()))
                .andExpect(jsonPath("$.serviceType").value("TERAPIA_FISICA"))
                .andExpect(jsonPath("$.availableSlotsCount").value(1))
                .andExpect(jsonPath("$.slots[0].bookingPayload.facilityId").value(facilityId.toString()));
    }

    @Test
    void shouldCreateAppointmentForN8n() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        AppointmentDTO dto = new AppointmentDTO(
                UUID.randomUUID(), patientId, doctorId, facilityId, null, scheduleId, date, time, 30,
                AppointmentType.PRESENCIAL, AppointmentStatus.SCHEDULED, "Checkup", null, LocalDateTime.now(), null
        );

        when(n8nPatientIntegrationService.createAppointment(any())).thenReturn(new N8nPatientAppointmentResponse(dto, "Cita agendada correctamente para atención por chat n8n."));

        String body = asJson(Map.of(
                "patientId", patientId,
                "doctorId", doctorId,
                "facilityId", facilityId,
                "scheduleId", scheduleId,
                "appointmentDate", date,
                "appointmentTime", time,
                "appointmentType", "PRESENCIAL",
                "reason", "Checkup"
        ));

        mockMvc.perform(post("/api/v1/integrations/n8n/patient/appointments")
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointment.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.summary").value("Cita agendada correctamente para atención por chat n8n."));
    }

    @Test
    void shouldCancelAppointmentForN8n() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        AppointmentDTO dto = new AppointmentDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30,
                AppointmentType.PRESENCIAL, AppointmentStatus.CANCELLED, "Cambio de plan", null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(n8nPatientIntegrationService.cancelAppointment(any(), any())).thenReturn(new N8nCancelAppointmentResponse(dto, "Cita cancelada correctamente desde el flujo n8n."));

        String body = asJson(Map.of("reason", "Cambio de plan"));

        mockMvc.perform(post("/api/v1/integrations/n8n/patient/appointments/{appointmentId}/cancel", appointmentId)
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointment.status").value("CANCELLED"));
    }

    @Test
    void shouldAcceptWebhookEventReceipts() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        when(n8nPatientIntegrationService.handleWebhookEvent(any())).thenReturn(new N8nWebhookEventResponse(
                eventId,
                "APPOINTMENT_CREATED",
                aggregateId,
                false,
                "RECEIVED",
                "Event stored successfully",
                LocalDateTime.now()
        ));

        String body = asJson(Map.of(
                "eventId", eventId,
                "eventType", "APPOINTMENT_CREATED",
                "aggregateId", aggregateId,
                "source", "n8n",
                "payload", Map.of("appointmentId", eventId),
                "published", false
        ));

        mockMvc.perform(post("/api/v1/integrations/n8n/webhooks/events")
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("APPOINTMENT_CREATED"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }
}