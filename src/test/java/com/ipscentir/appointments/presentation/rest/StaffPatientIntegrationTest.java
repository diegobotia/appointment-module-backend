package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DireccionRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Contacto;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Direccion;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffPatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ContactoRepository contactoRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    private UUID patientId;
    private static final String NUM_IDENTIFICACION = "12345678";

    @BeforeEach
    void setUp() {
        pacienteRepository.deleteAll();
        contactoRepository.deleteAll();
        direccionRepository.deleteAll();

        UUID contactId = contactoRepository.save(Contacto.builder()
                .email("paciente.staff@example.com")
                .telefono("+573001234567")
                .build()).getId();
        UUID directionId = direccionRepository.save(Direccion.builder()
                .codMunicipio("050001")
                .codZonaTerritorial("ZONA_1")
                .detalle("Calle 10 # 20-30")
                .build()).getId();

        patientId = pacienteRepository.save(Paciente.builder()
                .nombres("Juan")
                .apellidos("Pérez")
                .numIdentificacion(NUM_IDENTIFICACION)
                .codTipoIdentificacion("13")
                .fechaNacimiento(LocalDate.of(1990, 1, 15))
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

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "FACTURACION")
    void shouldRejectFacturacionRole() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void shouldRejectMedicoRole() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldSearchByNumIdentificacionOnly() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.nombres").value("Juan"))
                .andExpect(jsonPath("$.apellidos").value("Pérez"))
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"))
                .andExpect(jsonPath("$.codTipoIdentificacion").value("13"))
                .andExpect(jsonPath("$.tipoIdentificacionDescripcion").value("Cédula de ciudadanía"))
                .andExpect(jsonPath("$.numIdentificacion").value(NUM_IDENTIFICACION));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldSearchWithDianCodigo() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION)
                        .param("codTipoIdentificacion", "13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldSearchWithDescripcionTipoDocumento() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION)
                        .param("codTipoIdentificacion", "Cédula de ciudadanía"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldSearchWithLegacyAliasCc() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION)
                        .param("codTipoIdentificacion", "CC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()));
    }

    @Test
    @WithMockUser(roles = "ASESOR")
    void shouldAllowAsesorRole() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRACION")
    void shouldAllowAdministracionRole() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", NUM_IDENTIFICACION))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldReturnNotFoundWhenPatientDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", "00000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldRejectBlankNumIdentificacion() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/search")
                        .param("numIdentificacion", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("numIdentificacion es requerido"));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldReturnPatientDetailById() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/{id}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.numIdentificacion").value(NUM_IDENTIFICACION));
    }

    @Test
    @WithMockUser(roles = "ADMISIONES")
    void shouldReturnNotFoundForUnknownPatientId() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
