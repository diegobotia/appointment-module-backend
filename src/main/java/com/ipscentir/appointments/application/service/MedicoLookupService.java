package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.medico.MedicoPageResponse;
import com.ipscentir.appointments.application.dto.medico.MedicoSearchCriteria;
import com.ipscentir.appointments.application.dto.medico.MedicoSummaryDTO;
import com.ipscentir.appointments.application.exception.MedicoNotFoundException;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.MedicoEspecialidadRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Punto único de lectura de médicos ({@code hc.medicos}).
 */
@Service
@RequiredArgsConstructor
public class MedicoLookupService {

    private final SpecialistJpaRepository specialistJpaRepository;
    private final MedicoEspecialidadRepository medicoEspecialidadRepository;

    public Specialist requireById(@NonNull String medicoId) {
        String medicoIdValue = Objects.requireNonNull(medicoId, "medicoId");
        return specialistJpaRepository.findById(medicoIdValue)
                .orElseThrow(() -> new MedicoNotFoundException(medicoId));
    }

    public Specialist requireActiveById(@NonNull String medicoId) {
        Specialist specialist = requireById(medicoId);
        if (!specialist.isActive()) {
            throw new MedicoNotFoundException(medicoId);
        }
        return specialist;
    }

    @SuppressWarnings("null")
    public Optional<Specialist> findById(String medicoId) {
        if (medicoId == null || medicoId.isBlank()) {
            return Optional.empty();
        }
        String medicoIdValue = medicoId.trim();
        return specialistJpaRepository.findById(medicoIdValue);
    }

    public List<Specialist> findAllActive() {
        return specialistJpaRepository.findAllByActiveTrue();
    }

    public MedicoPageResponse search(MedicoSearchCriteria criteria) {
        int page = Math.max(criteria.page(), 0);
        int size = criteria.size() <= 0 ? 20 : Math.min(criteria.size(), 100);
        Boolean activeFilter = criteria.active() != null ? criteria.active() : Boolean.TRUE;

        Page<Specialist> result = specialistJpaRepository.search(
                normalize(criteria.q()),
                normalize(criteria.numDoc()),
                normalize(criteria.registro()),
                normalize(criteria.specialty()),
                activeFilter,
            PageRequest.of(page, size)
        );

        return new MedicoPageResponse(
                result.getContent().stream().map(this::toSummary).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public MedicoSummaryDTO getSummaryById(@NonNull String medicoId) {
        return toSummary(requireById(medicoId));
    }

    public List<String> findActiveSpecialties(String medicoId) {
        UUID medicoUuid = parseUuid(medicoId);
        if (medicoUuid == null) {
            return List.of();
        }
        return medicoEspecialidadRepository.findActiveSpecialties(medicoUuid);
    }

    public Optional<String> findPrimarySpecialty(String medicoId) {
        return findActiveSpecialties(medicoId).stream().findFirst();
    }

    public Map<String, String> resolveDisplayNames(Collection<String> medicoIds) {
        if (medicoIds == null || medicoIds.isEmpty()) {
            return Map.of();
        }
        return specialistJpaRepository.findAllById(medicoIds).stream()
                .collect(Collectors.toMap(
                        Specialist::getId,
                        MedicoLookupService::formatFullName,
                        (left, right) -> left
                ));
    }

    public MedicoSummaryDTO toSummary(Specialist specialist) {
        return new MedicoSummaryDTO(
                specialist.getId(),
                specialist.getFirstName(),
                specialist.getLastName(),
                formatFullName(specialist),
                specialist.getNumDoc(),
                specialist.getNumeroMedico(),
                findPrimarySpecialty(specialist.getId()).orElse(null),
                specialist.isActive()
        );
    }

    public static String formatFullName(Specialist specialist) {
        return (specialist.getFirstName() + " " + specialist.getLastName()).trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
