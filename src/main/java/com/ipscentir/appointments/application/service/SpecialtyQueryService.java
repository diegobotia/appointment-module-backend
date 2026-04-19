package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.specialist.SpecialistDTO;
import com.ipscentir.appointments.application.dto.specialty.SpecialtyDTO;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.domain.model.specialty.Specialty;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialtyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpecialtyQueryService {

    private final SpecialtyJpaRepository specialtyJpaRepository;
    private final SpecialistJpaRepository specialistJpaRepository;

    @Transactional(readOnly = true)
    public List<SpecialtyDTO> listSpecialties() {
        return specialtyJpaRepository.findAllByActiveTrueOrderByDisplayNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SpecialistDTO> listSpecialistsBySpecialty(UUID specialtyId, boolean activeOnly, Pageable pageable) {
        specialtyJpaRepository.findById(specialtyId)
                .orElseThrow(() -> new IllegalArgumentException("Specialty not found"));

        Page<Specialist> specialists = activeOnly
                ? specialistJpaRepository.findDistinctBySpecialties_IdAndActiveTrue(specialtyId, pageable)
                : specialistJpaRepository.findDistinctBySpecialties_Id(specialtyId, pageable);

        return specialists.map(this::toSpecialistDto);
    }

    private SpecialtyDTO toDto(Specialty specialty) {
        return new SpecialtyDTO(
                specialty.getId(),
                specialty.getCode(),
                specialty.getDisplayName(),
                specialty.isActive()
        );
    }

    private SpecialistDTO toSpecialistDto(Specialist specialist) {
        return new SpecialistDTO(
                specialist.getId(),
                specialist.getFirstName(),
                specialist.getLastName(),
                specialist.getEmail(),
                specialist.isActive()
        );
    }
}