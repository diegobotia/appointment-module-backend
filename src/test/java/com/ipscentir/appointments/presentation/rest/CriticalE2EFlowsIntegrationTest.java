package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DireccionRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DomainEventJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Contacto;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Direccion;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        @Autowired
        private SedeJpaRepository sedeJpaRepository;

        @Autowired
        private PacienteRepository pacienteRepository;

        @Autowired
        private ContactoRepository contactoRepository;

        @Autowired
        private DireccionRepository direccionRepository;

        private static final MediaType JSON = MediaType.APPLICATION_JSON;

        @BeforeEach
        void setUp() {
                appointmentJpaRepository.deleteAll();
                domainEventJpaRepository.deleteAll();
                scheduleJpaRepository.deleteAll();
                specialistJpaRepository.deleteAll();
                pacienteRepository.deleteAll();
                contactoRepository.deleteAll();
                direccionRepository.deleteAll();
        }


        @Test
        void e2eN8nPatientHappyFlowWithCancellationAndEventJournal() throws Exception {
                String doctorId = UUID.randomUUID().toString();
                Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
                LocalDate date = nextOpenWeekday(LocalDate.now().plusDays(2));
                LocalTime time = LocalTime.of(9, 0);

                UUID patientId = seedPatient("CC", "1122334455");

                Schedule schedule = scheduleJpaRepository.save(Schedule.builder()
                                .doctorId(doctorId)
                                .sedeId(sedeId)
                                .specialty("Medico fisiatria")
                                .dayOfWeek(date.getDayOfWeek())
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(12, 0))
                                .slotDurationMinutes(30)
                                .maxPatientsPerSlot(5)
                                .isActive(true)
                                .build());

                mockMvc.perform(post("/api/v1/integrations/n8n/patient/identify")
                                .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                                .contentType(JSON)
                                .content(asJson(Map.of(
                                                "codTipoIdentificacion", "CC",
                                                "numIdentificacion", "1122334455"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.found").value(true))
                                .andExpect(jsonPath("$.patientId").value(patientId.toString()));

                mockMvc.perform(post("/api/v1/integrations/n8n/patient/availability")
                                .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                                .contentType(JSON)
                                .content(asJson(Map.of(
                                                "doctorId", doctorId,
                                                "facilityId", "BELEN",
                                                "serviceType", "MEDICO_FISIATRIA",
                                                "date", date))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.availableSlotsCount")
                                                .value(org.hamcrest.Matchers.greaterThan(0)));

                MvcResult createdResult = mockMvc.perform(post("/api/v1/integrations/n8n/patient/appointments")
                                .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                                .contentType(JSON)
                                .content(asJson(Map.of(
                                                "patientId", patientId,
                                                "doctorId", doctorId,
                                                "facilityId", "BELEN",
                                                "scheduleId", schedule.getId(),
                                                "appointmentDate", date,
                                                "appointmentTime", time,
                                                "conversationId", "e2e-conv-001",
                                                "reason", "Control general"))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.appointment.status").value("SCHEDULED"))
                                .andReturn();

                UUID appointmentId = UUID.fromString(readField(createdResult, "appointment.id"));

                mockMvc.perform(get("/api/v1/integrations/n8n/patient/appointments")
                                .header(N8N_API_KEY_HEADER, N8N_API_KEY)
                                .param("codTipoIdentificacion", "CC")
                                .param("numIdentificacion", "1122334455"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.total").value(1));

                mockMvc.perform(post("/api/v1/integrations/n8n/patient/appointments/{appointmentId}/cancel",
                                appointmentId)
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

        private UUID seedPatient(String codTipo, String numId) {
                UUID contactId = contactoRepository.save(Contacto.builder()
                                .email("e2e@example.com")
                                .telefono("+573001112233")
                                .build()).getId();
                UUID directionId = direccionRepository.save(Direccion.builder()
                                .codMunicipio("050001")
                                .codZonaTerritorial("ZONA_1")
                                .detalle("Calle 1 # 2-3")
                                .build()).getId();
                return pacienteRepository.save(Paciente.builder()
                                .nombres("E2E")
                                .apellidos("Paciente")
                                .numIdentificacion(numId)
                                .codTipoIdentificacion(codTipo)
                                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                                .idGenero(UUID.randomUUID())
                                .idEstadoCivil(UUID.randomUUID())
                                .idOcupacion(UUID.randomUUID())
                                .idGrupoSanguineo(UUID.randomUUID())
                                .idEscolaridad(UUID.randomUUID())
                                .estrato((short) 2)
                                .idPaisOrigen(1L)
                                .idContacto(contactId)
                                .idDireccion(directionId)
                                .build()).getId();
        }

        private static LocalDate nextOpenWeekday(LocalDate date) {
                while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        date = date.plusDays(1);
                }
                return date;
        }
}