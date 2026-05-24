package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.exception.PatientNotFoundException;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffPatientLookupServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private TipoIdentificacionResolver tipoIdentificacionResolver;

    @InjectMocks
    private StaffPatientLookupService staffPatientLookupService;

    @Test
    void searchByDocumentUsesResolverWhenCodTipoProvided() {
        Paciente paciente = samplePaciente("13", "12345678");
        when(tipoIdentificacionResolver.findPaciente("Cédula de ciudadanía", "12345678"))
                .thenReturn(Optional.of(paciente));
        when(tipoIdentificacionResolver.resolveDescripcion("13"))
                .thenReturn("Cédula de ciudadanía");

        var result = staffPatientLookupService.searchByDocument("Cédula de ciudadanía", "12345678");

        assertEquals(paciente.getId(), result.id());
        assertEquals("Juan Pérez", result.fullName());
        assertEquals("Cédula de ciudadanía", result.tipoIdentificacionDescripcion());
        verify(tipoIdentificacionResolver).findPaciente("Cédula de ciudadanía", "12345678");
    }

    @Test
    void searchByDocumentUsesNumIdentificacionOnlyWhenCodTipoMissing() {
        Paciente paciente = samplePaciente("13", "87654321");
        when(pacienteRepository.findByNumIdentificacion("87654321")).thenReturn(Optional.of(paciente));
        when(tipoIdentificacionResolver.resolveDescripcion("13"))
                .thenReturn("Cédula de ciudadanía");

        var result = staffPatientLookupService.searchByDocument(null, "87654321");

        assertEquals("87654321", result.numIdentificacion());
        verify(pacienteRepository).findByNumIdentificacion("87654321");
    }

    @Test
    void searchByDocumentThrowsWhenNumIdentificacionBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> staffPatientLookupService.searchByDocument("13", "  "));
    }

    @Test
    void searchByDocumentThrowsWhenPatientMissing() {
        when(pacienteRepository.findByNumIdentificacion("000")).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> staffPatientLookupService.searchByDocument(null, "000"));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(pacienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> staffPatientLookupService.getById(id));
    }

    private Paciente samplePaciente(String codTipo, String numIdentificacion) {
        return Paciente.builder()
                .id(UUID.randomUUID())
                .nombres("Juan")
                .apellidos("Pérez")
                .codTipoIdentificacion(codTipo)
                .numIdentificacion(numIdentificacion)
                .build();
    }
}
