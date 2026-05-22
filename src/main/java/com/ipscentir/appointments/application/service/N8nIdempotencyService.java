package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.integration.N8nIdempotencyRecord;
import com.ipscentir.appointments.infrastructure.persistence.jpa.N8nIdempotencyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class N8nIdempotencyService {

    public static final String SCOPE_BOOK_APPOINTMENT = "BOOK_APPOINTMENT";
    public static final String SCOPE_RESCHEDULE_APPOINTMENT = "RESCHEDULE_APPOINTMENT";

    private final N8nIdempotencyJpaRepository repository;

    @Transactional(readOnly = true)
    public Optional<UUID> findAppointmentId(String scope, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByScopeAndIdempotencyKey(scope, idempotencyKey)
                .map(N8nIdempotencyRecord::getAppointmentId);
    }

    @Transactional
    public void storeAppointmentBooking(String idempotencyKey, UUID appointmentId) {
        store(SCOPE_BOOK_APPOINTMENT, idempotencyKey, appointmentId);
    }

    @Transactional
    public void storeAppointmentBooking(String scope, String idempotencyKey, UUID appointmentId) {
        store(scope, idempotencyKey, appointmentId);
    }

    private void store(String scope, String idempotencyKey, UUID appointmentId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            repository.save(N8nIdempotencyRecord.builder()
                    .scope(scope)
                    .idempotencyKey(idempotencyKey.trim())
                    .appointmentId(appointmentId)
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            // Carrera concurrente: la otra transacción ya persistió la clave.
        }
    }
}
