package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.form.CreatePqrsRequest;
import com.ipscentir.appointments.application.dto.form.PqrsResponse;
import com.ipscentir.appointments.application.mapper.PqrsMapper;
import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsType;
import com.ipscentir.appointments.domain.repository.PqrsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PqrsApplicationServiceTest {

    @Mock
    private PqrsRepository pqrsRepository;

    @Mock
    private PqrsMapper pqrsMapper;

    @InjectMocks
    private PqrsApplicationService pqrsApplicationService;

    @Test
    void shouldCreatePqrsWithValidRequest() {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "1234567890",
                "QUEJA",
                "Esta es una queja detallada sobre el servicio recibido",
                "user@example.com",
                "Juan Pérez",
                "3001234567"
        );

        Pqrs pqrs = Pqrs.builder()
                .id(UUID.randomUUID())
                .cedula("1234567890")
                .tipo(PqrsType.QUEJA)
                .descripcion("Esta es una queja detallada sobre el servicio recibido")
                .correo("user@example.com")
                .nombres("Juan Pérez")
                .telefono("3001234567")
                .radicado("PQRS-2026-000001")
                .status(PqrsStatus.CREADO)
                .build();

        when(pqrsRepository.countByYearCreated(Year.now().getValue())).thenReturn(0L);
        when(pqrsMapper.toDomain(any(), anyString())).thenReturn(pqrs);
        when(pqrsRepository.save(any())).thenReturn(pqrs);

        PqrsResponse response = pqrsApplicationService.createPqrs(request);

        assertNotNull(response);
        assertEquals("PQRS-2026-000001", response.radicado());
        assertEquals(PqrsStatus.CREADO.name(), response.status());
        assertEquals("1234567890", response.cedula());
        assertTrue(response.message().contains("registrada"));

        verify(pqrsRepository).countByYearCreated(Year.now().getValue());
        verify(pqrsRepository).save(any());
    }

    @Test
    void shouldGenerateSequentialRadicado() {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "9876543210",
                "RECLAMO",
                "Esta es un reclamo detallado sobre lo que pasó",
                "another@example.com",
                "María García",
                "3009876543"
        );

        Pqrs pqrs = Pqrs.builder()
                .id(UUID.randomUUID())
                .cedula("9876543210")
                .tipo(PqrsType.RECLAMO)
                .descripcion("Esta es un reclamo detallado sobre lo que pasó")
                .correo("another@example.com")
                .nombres("María García")
                .telefono("3009876543")
                .radicado("PQRS-2026-000005")
                .status(PqrsStatus.CREADO)
                .build();

        // Simulate that there are already 4 PQRS in the current year
        when(pqrsRepository.countByYearCreated(Year.now().getValue())).thenReturn(4L);
        when(pqrsMapper.toDomain(any(), anyString())).thenReturn(pqrs);
        when(pqrsRepository.save(any())).thenReturn(pqrs);

        PqrsResponse response = pqrsApplicationService.createPqrs(request);

        assertNotNull(response);
        assertEquals("PQRS-2026-000005", response.radicado());
        verify(pqrsRepository).save(any());
    }

    @Test
    void shouldPersistPqrsWithAllFields() {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "1111111111",
                "SUGERENCIA",
                "Sugiero mejorar el proceso de atención al paciente con implementación de sistema de citas online",
                "suggestion@example.com",
                "Carlos López",
                "3005551234"
        );

        Pqrs pqrs = Pqrs.builder()
                .id(UUID.randomUUID())
                .cedula("1111111111")
                .tipo(PqrsType.SUGERENCIA)
                .descripcion("Sugiero mejorar el proceso de atención al paciente con implementación de sistema de citas online")
                .correo("suggestion@example.com")
                .nombres("Carlos López")
                .telefono("3005551234")
                .radicado("PQRS-2026-000010")
                .status(PqrsStatus.CREADO)
                .build();

        when(pqrsRepository.countByYearCreated(Year.now().getValue())).thenReturn(9L);
        when(pqrsMapper.toDomain(any(), anyString())).thenReturn(pqrs);
        when(pqrsRepository.save(any())).thenReturn(pqrs);

        pqrsApplicationService.createPqrs(request);

        verify(pqrsMapper).toDomain(request, "PQRS-2026-000010");
        verify(pqrsRepository).save(pqrs);
    }

    @Test
    void shouldGenerateRadicadoWithLeadingZeros() {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "1234567890",
                "PETICION",
                "Esta es una petición bien detallada sobre lo que necesito del servicio",
                "user@example.com",
                "Test User",
                "3001234567"
        );

        Pqrs pqrs = Pqrs.builder()
                .id(UUID.randomUUID())
                .cedula("1234567890")
                .tipo(PqrsType.PETICION)
                .descripcion("Esta es una petición bien detallada sobre lo que necesito del servicio")
                .correo("user@example.com")
                .nombres("Test User")
                .telefono("3001234567")
                .radicado("PQRS-2026-000001")
                .status(PqrsStatus.CREADO)
                .build();

        when(pqrsRepository.countByYearCreated(Year.now().getValue())).thenReturn(0L);
        when(pqrsMapper.toDomain(any(), anyString())).thenReturn(pqrs);
        when(pqrsRepository.save(any())).thenReturn(pqrs);

        PqrsResponse response = pqrsApplicationService.createPqrs(request);

        // Verify radicado has format PQRS-YYYY-NNNNNN
        assertTrue(response.radicado().matches("PQRS-\\d{4}-\\d{6}"));
    }

    @Test
    void shouldReturnResponseWithCreatedTimestamp() {
        CreatePqrsRequest request = new CreatePqrsRequest(
                "1234567890",
                "QUEJA",
                "Queja detallada sobre el servicio en la sala de espera",
                "user@example.com",
                "Usuario Test",
                "3001234567"
        );

        Pqrs pqrs = Pqrs.builder()
                .id(UUID.randomUUID())
                .cedula("1234567890")
                .tipo(PqrsType.QUEJA)
                .descripcion("Queja detallada sobre el servicio en la sala de espera")
                .correo("user@example.com")
                .nombres("Usuario Test")
                .telefono("3001234567")
                .radicado("PQRS-2026-000001")
                .status(PqrsStatus.CREADO)
                .build();

        when(pqrsRepository.countByYearCreated(Year.now().getValue())).thenReturn(0L);
        when(pqrsMapper.toDomain(any(), anyString())).thenReturn(pqrs);
        when(pqrsRepository.save(any())).thenAnswer(invocation -> {
            Pqrs saved = invocation.getArgument(0);
            // Simulate database setting createdAt via reflection
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            return saved;
        });

        PqrsResponse response = pqrsApplicationService.createPqrs(request);

        assertNotNull(response.createdAt());
    }
}

