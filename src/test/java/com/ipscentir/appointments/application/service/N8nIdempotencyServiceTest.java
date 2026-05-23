package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.integration.N8nIdempotencyRecord;
import com.ipscentir.appointments.infrastructure.persistence.jpa.N8nIdempotencyJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class N8nIdempotencyServiceTest {

    @Mock
    private N8nIdempotencyJpaRepository repository;

    @InjectMocks
    private N8nIdempotencyService n8nIdempotencyService;

    @Test
    void findAppointmentIdReturnsEmptyForBlankKey() {
        assertTrue(n8nIdempotencyService.findAppointmentId(N8nIdempotencyService.SCOPE_BOOK_APPOINTMENT, "  ").isEmpty());
    }

    @Test
    void findAppointmentIdReturnsStoredAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(repository.findByScopeAndIdempotencyKey(N8nIdempotencyService.SCOPE_BOOK_APPOINTMENT, "key-1"))
                .thenReturn(Optional.of(N8nIdempotencyRecord.builder()
                        .scope(N8nIdempotencyService.SCOPE_BOOK_APPOINTMENT)
                        .idempotencyKey("key-1")
                        .appointmentId(appointmentId)
                        .build()));

        Optional<UUID> result = n8nIdempotencyService.findAppointmentId(
                N8nIdempotencyService.SCOPE_BOOK_APPOINTMENT, "key-1");

        assertEquals(appointmentId, result.orElseThrow());
    }

    @Test
    void storeAppointmentBookingPersistsRecord() {
        UUID appointmentId = UUID.randomUUID();
        n8nIdempotencyService.storeAppointmentBooking("  conv-99  ", appointmentId);

        ArgumentCaptor<N8nIdempotencyRecord> captor = ArgumentCaptor.forClass(N8nIdempotencyRecord.class);
        verify(repository).save(captor.capture());
        assertEquals("conv-99", captor.getValue().getIdempotencyKey());
        assertEquals(appointmentId, captor.getValue().getAppointmentId());
    }

    @Test
    void storeIgnoresDuplicateKeyRace() {
        doThrow(new DataIntegrityViolationException("duplicate")).when(repository).save(any());
        n8nIdempotencyService.storeAppointmentBooking("dup", UUID.randomUUID());
    }
}
