package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.PqrsDTO;
import com.ipscentir.appointments.application.dto.form.CreatePqrsRequest;
import com.ipscentir.appointments.application.dto.form.PqrsResponse;
import com.ipscentir.appointments.application.mapper.PqrsMapper;
import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import com.ipscentir.appointments.domain.repository.PqrsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class PqrsApplicationService {

    private final PqrsRepository pqrsRepository;
    private final PqrsMapper pqrsMapper;

    @Transactional
    public PqrsResponse createPqrs(CreatePqrsRequest request) {
        // Generar radicado secuencial anual
        String radicado = generateRadicado();

        // Mapear request a entity
        Pqrs pqrs = pqrsMapper.toDomain(request, radicado);

        // Persistir
        Pqrs saved = pqrsRepository.save(pqrs);

        // Retornar respuesta
        return new PqrsResponse(
                saved.getRadicado(),
                saved.getStatus().name(),
                "Su PQRS ha sido registrada correctamente",
                saved.getCreatedAt(),
                saved.getCedula()
        );
    }

    @Transactional(readOnly = true)
    public PqrsDTO getPqrsById(java.util.UUID id) {
        Pqrs pqrs = pqrsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PQRS no encontrada con ID: " + id));
        return pqrsMapper.toDto(pqrs);
    }

    @Transactional(readOnly = true)
    public PqrsDTO getPqrsByRadicado(String radicado) {
        Pqrs pqrs = pqrsRepository.findByRadicado(radicado)
                .orElseThrow(() -> new IllegalArgumentException("PQRS no encontrada con radicado: " + radicado));
        return pqrsMapper.toDto(pqrs);
    }

    private String generateRadicado() {
        int year = Year.now().getValue();
        long count = pqrsRepository.countByYearCreated(year);
        long nextSequential = count + 1;
        return String.format("PQRS-%d-%06d", year, nextSequential);
    }
}
