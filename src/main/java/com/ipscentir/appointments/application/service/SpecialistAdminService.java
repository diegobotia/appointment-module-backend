package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.specialist.SpecialistDTO;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * SpecialistAdminService - Solo lectura
 * 
 * NOTA: La creación/modificación de médicos ocurre en el sistema HC (health center).
 * Este servicio solo proporciona acceso de lectura a los médicos desde hc.medicos.
 * 
 * Las especialidades se manejan mediante:
 * - hc.medicos.especialidad (campo directo)
 * - specialist_metadata (tabla para mapeos enriquecidos si es necesario)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpecialistAdminService {

    private static final String SPECIALIST_NOT_FOUND = "Specialist not found";

    private final SpecialistJpaRepository specialistJpaRepository;

    @Transactional(readOnly = true)
    public SpecialistDTO getById(String specialistId) {
        return specialistJpaRepository.findById(Objects.requireNonNull(specialistId, "specialistId"))
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException(SPECIALIST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<SpecialistDTO> listAll() {
        return specialistJpaRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialistDTO> listActive() {
        return specialistJpaRepository.findAllByActiveTrue().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpecialistDTO getByMedicoNumber(String numeroMedico) {
        return specialistJpaRepository.findByNumeroMedico(numeroMedico)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Specialist with medico number '" + numeroMedico + "' not found"));
    }

    private SpecialistDTO toDto(Specialist specialist) {
        return new SpecialistDTO(
                specialist.getId(),
                specialist.getFirstName(),
                specialist.getLastName(),
                specialist.isActive()
        );
    }
}
