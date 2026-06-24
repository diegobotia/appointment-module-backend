package com.ipscentir.appointments.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentResourceAllocationJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DireccionRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentListEnrichmentIntegrationTest {

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

    @Autowired
    private SpecialistJpaRepository specialistJpaRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ContactoRepository contactoRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    private String doctorId;
    private String secondaryDoctorId;
    private UUID patientId;
    private Integer sedeId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        allocationJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();
        specialistJpaRepository.deleteAll();
        pacienteRepository.deleteAll();
        contactoRepository.deleteAll();
        direccionRepository.deleteAll();

        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        appointmentDate = nextWeekday(LocalDate.now().plusDays(6));
        appointmentTime = LocalTime.of(10, 30);

        doctorId = specialistJpaRepository.save(Specialist.builder()
                .id(UUID.randomUUID().toString())
                .numeroMedico("ENR-001")
                .firstName("Diana")
                .lastName("Vargas")
                .specialty("Dolor")
                .active(true)
                .build()).getId();

        secondaryDoctorId = specialistJpaRepository.save(Specialist.builder()
                .id(UUID.randomUUID().toString())
                .numeroMedico("ENR-002")
                .firstName("Carlos")
                .lastName("Mejia")
                .specialty("Psicologia")
                .active(true)
                .build()).getId();

        patientId = seedPatient("Maria", "Lopez", "900100200");

        scheduleId = scheduleJpaRepository.save(Schedule.builder()
                .doctorId(doctorId)
                .sedeId(sedeId)
                .specialty("Medicina general")
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build()).getId();

        seedSchedule(secondaryDoctorId);
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void listAppointmentsShouldIncludeDisplayNamesAndAdministrativeFlag() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patientId,
                                "doctorId", doctorId,
                                "sedeId", sedeId,
                                "scheduleId", scheduleId,
                                "appointmentDate", appointmentDate,
                                "appointmentTime", appointmentTime,
                                "appointmentType", "PRESENCIAL",
                                "reason", "Control"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/appointments/administrative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "participantDoctorIds", List.of(doctorId, secondaryDoctorId),
                                "sedeId", sedeId,
                                "appointmentDate", appointmentDate,
                                "appointmentTime", LocalTime.of(11, 0),
                                "durationMinutes", 60,
                                "reason", "Junta interna"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments")
                        .param("sedeId", sedeId.toString())
                        .param("fromDate", appointmentDate.toString())
                        .param("toDate", appointmentDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[?(@.appointmentType == 'PRESENCIAL')].medicoDisplayName")
                        .value("Diana Vargas"))
                .andExpect(jsonPath("$.content[?(@.appointmentType == 'PRESENCIAL')].patientDisplayName")
                        .value("Maria Lopez"))
                .andExpect(jsonPath("$.content[?(@.appointmentType == 'PRESENCIAL')].administrative")
                        .value(false))
                .andExpect(jsonPath("$.content[?(@.appointmentType == 'STAFF')].medicoDisplayName")
                        .value("Diana Vargas"))
                .andExpect(jsonPath("$.content[?(@.appointmentType == 'STAFF')][0].patientId").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.appointmentType == 'STAFF')][0].patientDisplayName").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.appointmentType == 'STAFF')].administrative")
                        .value(true));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void getAppointmentByIdShouldReturnEnrichedFields() throws Exception {
        var created = mockMvc.perform(post("/api/v1/appointments/administrative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "participantDoctorIds", List.of(doctorId),
                                "sedeId", sedeId,
                                "appointmentDate", appointmentDate,
                                "appointmentTime", LocalTime.of(14, 0),
                                "durationMinutes", 30,
                                "reason", "Bloqueo"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicoDisplayName").value("Diana Vargas"))
                .andExpect(jsonPath("$.patientDisplayName").value(nullValue()))
                .andExpect(jsonPath("$.administrative").value(true))
                .andReturn();

        UUID appointmentId = UUID.fromString(
                objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asText());

        mockMvc.perform(get("/api/v1/appointments/{appointmentId}", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicoDisplayName").value("Diana Vargas"))
                .andExpect(jsonPath("$.patientDisplayName").value(nullValue()))
                .andExpect(jsonPath("$.administrative").value(true));
    }

    private UUID seedPatient(String nombres, String apellidos, String numIdentificacion) {
        UUID contactId = contactoRepository.save(Contacto.builder()
                .email("enrichment@example.com")
                .telefono("+573009991122")
                .build()).getId();
        UUID directionId = direccionRepository.save(Direccion.builder()
                .codMunicipio("050001")
                .codZonaTerritorial("ZONA_1")
                .detalle("Calle 10 # 20-30")
                .build()).getId();
        return pacienteRepository.save(Paciente.builder()
                .nombres(nombres)
                .apellidos(apellidos)
                .numIdentificacion(numIdentificacion)
                .codTipoIdentificacion("CC")
                .fechaNacimiento(LocalDate.of(1990, 3, 12))
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

    private void seedSchedule(String participantDoctorId) {
        scheduleJpaRepository.save(Schedule.builder()
                .doctorId(participantDoctorId)
                .sedeId(sedeId)
                .specialty("Staff")
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build());
    }

    /** Evita sábado (cierre 12:00) y domingo (cerrado) del seed operativo de sedes. */
    private static LocalDate nextWeekday(LocalDate date) {
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
