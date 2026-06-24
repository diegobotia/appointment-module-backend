package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DireccionRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffAppointmentLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ContactoRepository contactoRepository;

    @Autowired
    private DireccionRepository direccionRepository;

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
        appointmentJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();
        pacienteRepository.deleteAll();
        contactoRepository.deleteAll();
        direccionRepository.deleteAll();

        patientId = seedPatient();
        doctorId = UUID.randomUUID().toString();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        appointmentDate = LocalDate.now().plusDays(5);
        appointmentTime = LocalTime.of(10, 0);

        scheduleId = scheduleJpaRepository.save(Schedule.builder()
                .doctorId(doctorId)
                .sedeId(sedeId)
                .specialty("Medicina general")
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build()).getId();
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "ADMISIONES")
    void shouldRunStaffLifecycleConfirmCheckInComplete() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patientId,
                                "doctorId", doctorId,
                                "sedeId", sedeId,
                                "scheduleId", scheduleId,
                                "appointmentDate", appointmentDate,
                                "appointmentTime", appointmentTime,
                                "appointmentType", "PRESENCIAL",
                                "reason", "Consulta mostrador"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();

        UUID appointmentId = UUID.fromString(
                objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asText()
        );

        mockMvc.perform(patch("/api/v1/appointments/{id}/confirm", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/api/v1/appointments/{id}/check-in", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private UUID seedPatient() {
        UUID contactId = contactoRepository.save(Contacto.builder()
                .email("staff-flow@example.com")
                .telefono("+573001112233")
                .build()).getId();
        UUID directionId = direccionRepository.save(Direccion.builder()
                .codMunicipio("050001")
                .codZonaTerritorial("ZONA_1")
                .detalle("Calle 1 # 2-3")
                .build()).getId();
        return pacienteRepository.save(Paciente.builder()
                .nombres("Luis")
                .apellidos("Diaz")
                .numIdentificacion("9988776655")
                .codTipoIdentificacion("CC")
                .fechaNacimiento(LocalDate.of(1985, 6, 15))
                .idGenero(UUID.randomUUID())
                .idEstadoCivil(UUID.randomUUID())
                .idOcupacion(UUID.randomUUID())
                .idGrupoSanguineo(UUID.randomUUID())
                .idEscolaridad(UUID.randomUUID())
                .estrato((short) 3)
                .idPaisOrigen(1L)
                .idContacto(contactId)
                .idDireccion(directionId)
                .build()).getId();
    }
}
