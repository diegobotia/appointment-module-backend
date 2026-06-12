package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentEnrichmentServiceTest {

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private MedicoLookupService medicoLookupService;

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private AppointmentEnrichmentService appointmentEnrichmentService;

    @Test
    void toDtosResolvesMedicoAndPatientDisplayNamesInBatch() {
        UUID patientId = UUID.randomUUID();
        String medicoId = "doc-1";
        Appointment clinical = clinicalAppointment(patientId, medicoId);
        Appointment administrative = administrativeAppointment("doc-2", "doc-3");

        when(appointmentMapper.toDto(clinical)).thenReturn(baseDto(clinical, false));
        when(appointmentMapper.toDto(administrative)).thenReturn(baseDto(administrative, true));

        when(medicoLookupService.resolveDisplayNames(Set.of("doc-1", "doc-2", "doc-3"))).thenReturn(Map.of(
                "doc-1", "Laura Gomez",
                "doc-2", "Pedro Ruiz"
        ));
        Paciente paciente = Paciente.builder()
                .id(patientId)
                .nombres("Juan")
                .apellidos("Perez")
                .numIdentificacion("123")
                .codTipoIdentificacion("CC")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .idGenero(UUID.randomUUID())
                .idEstadoCivil(UUID.randomUUID())
                .idOcupacion(UUID.randomUUID())
                .idGrupoSanguineo(UUID.randomUUID())
                .idEscolaridad(UUID.randomUUID())
                .estrato((short) 2)
                .idPaisOrigen(1L)
                .idContacto(UUID.randomUUID())
                .idDireccion(UUID.randomUUID())
                .build();
        when(pacienteRepository.findAllById(Set.of(patientId))).thenReturn(List.of(paciente));

        List<AppointmentDTO> result = appointmentEnrichmentService.toDtos(List.of(clinical, administrative));

        assertEquals(2, result.size());
        assertEquals("Laura Gomez", result.get(0).medicoDisplayName());
        assertEquals("Juan Perez", result.get(0).patientDisplayName());
        assertFalse(result.get(0).administrative());

        assertEquals("Pedro Ruiz", result.get(1).medicoDisplayName());
        assertNull(result.get(1).patientDisplayName());
        assertTrue(result.get(1).administrative());

        verify(medicoLookupService).resolveDisplayNames(any());
        verify(pacienteRepository).findAllById(any());
    }

    @Test
    void toDtoLeavesDisplayNamesNullWhenReferencesAreMissing() {
        Appointment appointment = clinicalAppointment(UUID.randomUUID(), "missing-doc");
        when(appointmentMapper.toDto(appointment)).thenReturn(baseDto(appointment, false));
        when(medicoLookupService.resolveDisplayNames(Set.of("missing-doc"))).thenReturn(Map.of());
        when(pacienteRepository.findAllById(any())).thenReturn(List.of());

        AppointmentDTO result = appointmentEnrichmentService.toDto(appointment);

        assertNull(result.medicoDisplayName());
        assertNull(result.patientDisplayName());
    }

    private static Appointment clinicalAppointment(UUID patientId, String medicoId) {
        return Appointment.scheduleNew(
                patientId,
                medicoId,
                null,
                new AppointmentScheduleData(
                        UUID.randomUUID(),
                        FacilityMasterData.SEDE_ID_BELEN,
                        LocalDate.now().plusDays(1),
                        LocalTime.of(9, 0),
                        30,
                        AppointmentType.PRESENCIAL,
                        AppointmentStatus.SCHEDULED,
                        "Consulta"
                )
        );
    }

    private static Appointment administrativeAppointment(String primaryMedicoId, String secondaryMedicoId) {
        return Appointment.scheduleStaffMeeting(
                List.of(primaryMedicoId, secondaryMedicoId),
                new AppointmentScheduleData(
                        null,
                        FacilityMasterData.SEDE_ID_BELEN,
                        LocalDate.now().plusDays(2),
                        LocalTime.of(11, 0),
                        60,
                        AppointmentType.STAFF,
                        AppointmentStatus.SCHEDULED,
                        "Junta"
                ),
                BookingChannel.STAFF
        );
    }

    private static AppointmentDTO baseDto(Appointment appointment, boolean administrative) {
        return new AppointmentDTO(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getSedeId(),
                appointment.getAdditionalDoctorIds(),
                appointment.getScheduleId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getAppointmentType(),
                appointment.getStatus(),
                appointment.getBookingChannel(),
                appointment.getN8nConversationId(),
                appointment.getReason(),
                null,
                null,
                null,
                null,
                null,
                administrative
        );
    }
}
