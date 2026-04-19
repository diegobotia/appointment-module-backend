package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.specialist.AssignSpecialtyRequest;
import com.ipscentir.appointments.application.dto.specialist.CreateSpecialistRequest;
import com.ipscentir.appointments.application.dto.specialist.SpecialistDTO;
import com.ipscentir.appointments.application.dto.specialist.UpdateSpecialistRequest;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.domain.model.specialty.Specialty;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialtyJpaRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpecialistAdminService {

    private static final int MAX_SPECIALTIES_PER_SPECIALIST = 4;
    private static final String SPECIALIST_NOT_FOUND = "Specialist not found";
    private static final String SPECIALTY_NOT_FOUND = "Specialty not found";
    private static final String SPECIALTY_ALREADY_ASSIGNED = "Specialty already assigned to specialist";
    private static final String SPECIALTY_INACTIVE = "Specialty is inactive";
    private static final String SPECIALIST_SPECIALTY_LIMIT = "Specialist cannot have more than 4 specialties";
    private static final String SPECIALTY_NOT_ASSIGNED = "Specialty not assigned to specialist";

    private final SpecialistJpaRepository specialistJpaRepository;
    private final SpecialtyJpaRepository specialtyJpaRepository;

    @Transactional
    public SpecialistDTO create(CreateSpecialistRequest request) {
        specialistJpaRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("Specialist email already exists");
        });

        Specialist saved = specialistJpaRepository.save(
                Specialist.builder()
                        .firstName(request.firstName())
                        .lastName(request.lastName())
                        .email(request.email())
                        .active(true)
                        .build()
        );

        return toDto(saved);
    }

    @Transactional
    public SpecialistDTO update(UUID specialistId, UpdateSpecialistRequest request) {
        Specialist specialist = specialistJpaRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException(SPECIALIST_NOT_FOUND));

        if (specialistJpaRepository.existsByEmailAndIdNot(request.email(), specialistId)) {
            throw new IllegalArgumentException("Specialist email already exists");
        }

        specialist.updateProfile(request.firstName(), request.lastName(), request.email());
        return toDto(specialistJpaRepository.save(specialist));
    }

    @Transactional
    public SpecialistDTO deactivate(UUID specialistId) {
        Specialist specialist = specialistJpaRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException(SPECIALIST_NOT_FOUND));

        specialist.deactivate();
        return toDto(specialistJpaRepository.save(specialist));
    }

    @Transactional
    public SpecialistDTO assignSpecialty(UUID specialistId, AssignSpecialtyRequest request) {
        Specialist specialist = specialistJpaRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException(SPECIALIST_NOT_FOUND));

        Specialty specialty = specialtyJpaRepository.findById(request.specialtyId())
                .orElseThrow(() -> new IllegalArgumentException(SPECIALTY_NOT_FOUND));

        if (!specialty.isActive()) {
            throw new IllegalStateException(SPECIALTY_INACTIVE);
        }

        if (specialist.getSpecialties().contains(specialty)) {
            throw new IllegalStateException(SPECIALTY_ALREADY_ASSIGNED);
        }

        if (specialist.getSpecialties().size() >= MAX_SPECIALTIES_PER_SPECIALIST) {
            throw new IllegalStateException(SPECIALIST_SPECIALTY_LIMIT);
        }

        specialist.addSpecialty(specialty);
        Specialist saved = specialistJpaRepository.save(specialist);

        log.info("Assigned specialty {} to specialist {}", specialty.getCode(), specialist.getEmail());
        return toDto(saved);
    }

    @Transactional
    public SpecialistDTO removeSpecialty(UUID specialistId, UUID specialtyId) {
        Specialist specialist = specialistJpaRepository.findById(specialistId)
            .orElseThrow(() -> new IllegalArgumentException(SPECIALIST_NOT_FOUND));

        Specialty specialty = specialtyJpaRepository.findById(specialtyId)
            .orElseThrow(() -> new IllegalArgumentException(SPECIALTY_NOT_FOUND));

        if (!specialist.getSpecialties().contains(specialty)) {
            throw new IllegalArgumentException(SPECIALTY_NOT_ASSIGNED);
        }

        specialist.removeSpecialty(specialty);
        Specialist saved = specialistJpaRepository.save(specialist);

        log.info("Removed specialty {} from specialist {}", specialty.getCode(), specialist.getEmail());
        return toDto(saved);
    }

    private SpecialistDTO toDto(Specialist specialist) {
        return new SpecialistDTO(
                specialist.getId(),
                specialist.getFirstName(),
                specialist.getLastName(),
                specialist.getEmail(),
                specialist.isActive()
        );
    }
}
