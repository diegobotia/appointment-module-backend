package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.form.CreatePatientRegistrationRequest;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentResourceAllocationJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.N8nIdempotencyJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DireccionRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Contacto;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Direccion;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class N8nPatientPhase1IntegrationTest {

    private static final String N8N_API_KEY_HEADER = "X-API-Key";
    private static final String N8N_API_KEY = "test-n8n-api-key";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ContactoRepository contactoRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private AppointmentResourceAllocationJpaRepository allocationJpaRepository;

    @Autowired
    private N8nIdempotencyJpaRepository n8nIdempotencyJpaRepository;

    @Autowired
    private SedeJpaRepository sedeJpaRepository;

    private UUID patientId;
    private String doctorId;
    private Integer sedeId;
    private UUID scheduleId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    @BeforeEach
    void setUp() {
        n8nIdempotencyJpaRepository.deleteAll();
        allocationJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();
        pacienteRepository.deleteAll();
        contactoRepository.deleteAll();
        direccionRepository.deleteAll();

        patientId = seedPatient("CC", "9876543210");
        doctorId = UUID.randomUUID().toString();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        appointmentDate = nextOpenWeekday(LocalDate.now().plusDays(3));
        appointmentTime = LocalTime.of(9, 0);

        Schedule schedule = scheduleJpaRepository.save(Schedule.builder()
                .doctorId(doctorId)
                .sedeId(sedeId)
                .specialty("Medico fisiatria")
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(5)
                .isActive(true)
                .build());
        scheduleId = schedule.getId();
    }

    @Test
    void shouldIdentifyRegisteredPatient() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/n8n/patient/identify")
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "codTipoIdentificacion", "CC",
                                "numIdentificacion", "9876543210"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.tipoIdentificacion").value("Cédula de ciudadanía"));
    }

    @Test
    void shouldListAppointmentsByDocument() throws Exception {
        bookAppointment("conv-list-1", null);

        mockMvc.perform(get("/api/v1/integrations/n8n/patient/appointments")
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .param("codTipoIdentificacion", "CC")
                        .param("numIdentificacion", "9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.appointments[0].status").value("SCHEDULED"));
    }

    @Test
    void shouldReturnSameAppointmentForDuplicateIdempotencyKey() throws Exception {
        String conversationId = "conv-idempotent-001";

        MvcResult first = bookAppointment(conversationId, null);
        String firstId = objectMapper.readTree(first.getResponse().getContentAsString())
                .path("appointment").path("id").asText();

        MvcResult second = bookAppointment(conversationId, null);
        String secondId = objectMapper.readTree(second.getResponse().getContentAsString())
                .path("appointment").path("id").asText();

        assertThat(secondId).isEqualTo(firstId);
        assertThat(appointmentJpaRepository.count()).isEqualTo(1);
        assertThat(n8nIdempotencyJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectBookingForUnknownPatient() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/n8n/patient/appointments")
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", UUID.randomUUID(),
                                "doctorId", doctorId,
                                "facilityId", "BELEN",
                                "scheduleId", scheduleId,
                                "appointmentDate", appointmentDate,
                                "appointmentTime", appointmentTime,
                                "reason", "Consulta"
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRegisterPatientViaN8n() throws Exception {
        CreatePatientRegistrationRequest request = CreatePatientRegistrationRequest.builder()
                .email("n8n.paciente@example.com")
                .nombres("Pedro")
                .apellidos("Ramirez")
                .numIdentificacion("5555555555")
                .codTipoIdentificacion("CC")
                .fechaNacimiento(LocalDate.of(1988, 1, 20))
                .idGenero(UUID.randomUUID().toString())
                .idEstadoCivil(UUID.randomUUID().toString())
                .idOcupacion(UUID.randomUUID().toString())
                .idGrupoSanguineo(UUID.randomUUID().toString())
                .idEscolaridad(UUID.randomUUID().toString())
                .estrato(3)
                .idPaisOrigen(1L)
                .telefono("+573009998877")
                .direccionDetalle("Carrera 50 # 20-10")
                .codMunicipio("050001")
                .codZonaTerritorial("ZONA_1")
                .build();

        mockMvc.perform(post("/api/v1/integrations/n8n/patient/register")
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").exists());
    }

    private MvcResult bookAppointment(String conversationId, String requestId) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("patientId", patientId);
        body.put("doctorId", doctorId);
        body.put("facilityId", "BELEN");
        body.put("scheduleId", scheduleId);
        body.put("appointmentDate", appointmentDate);
        body.put("appointmentTime", appointmentTime);
        body.put("reason", "Control");
        if (conversationId != null) {
            body.put("conversationId", conversationId);
        }
        if (requestId != null) {
            body.put("requestId", requestId);
        }

        return mockMvc.perform(post("/api/v1/integrations/n8n/patient/appointments")
                        .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                        .contentType(JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private UUID seedPatient(String codTipo, String numId) {
        UUID contactId = contactoRepository.save(Contacto.builder()
                .email("paciente@example.com")
                .telefono("+573001112233")
                .build()).getId();
        UUID directionId = direccionRepository.save(Direccion.builder()
                .codMunicipio("050001")
                .codZonaTerritorial("ZONA_1")
                .detalle("Calle 1 # 2-3")
                .build()).getId();
        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .nombres("Maria")
                .apellidos("Gomez")
                .numIdentificacion(numId)
                .codTipoIdentificacion(codTipo)
                .fechaNacimiento(LocalDate.of(1990, 5, 1))
                .idGenero(UUID.randomUUID())
                .idEstadoCivil(UUID.randomUUID())
                .idOcupacion(UUID.randomUUID())
                .idGrupoSanguineo(UUID.randomUUID())
                .idEscolaridad(UUID.randomUUID())
                .estrato((short) 2)
                .idPaisOrigen(1L)
                .idContacto(contactId)
                .idDireccion(directionId)
                .build());
        return paciente.getId();
    }

    private static LocalDate nextOpenWeekday(LocalDate date) {
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
