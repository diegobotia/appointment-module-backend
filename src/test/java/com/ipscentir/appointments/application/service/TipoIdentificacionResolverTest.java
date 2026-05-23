package com.ipscentir.appointments.application.service;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoIdentificacionResolverTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private TipoIdentificacionResolver resolver;

    @Test
    void shouldFindPatientByDescriptionWhenStoredWithLegacyCode() {
        UUID patientId = UUID.randomUUID();
        Paciente paciente = Paciente.builder()
                .id(patientId)
                .codTipoIdentificacion("CC")
                .numIdentificacion("123456")
                .build();

        when(pacienteRepository.findByCodTipoIdentificacionAndNumIdentificacion(eq("13"), eq("123456")))
                .thenReturn(Optional.empty());
        when(pacienteRepository.findByCodTipoIdentificacionAndNumIdentificacion(eq("CC"), eq("123456")))
                .thenReturn(Optional.of(paciente));

        var found = resolver.findPaciente("Cédula de ciudadanía", "123456");

        assertTrue(found.isPresent());
        assertEquals(patientId, found.get().getId());
    }

    @Test
    void shouldResolveDescriptionFromCodigoAliasOrDescription() {
        assertEquals("Cédula de ciudadanía", resolver.resolveDescripcion("CC"));
        assertEquals("Cédula de ciudadanía", resolver.resolveDescripcion("13"));
        assertEquals("Cédula de ciudadanía", resolver.resolveDescripcion("Cédula de ciudadanía"));
    }
}
