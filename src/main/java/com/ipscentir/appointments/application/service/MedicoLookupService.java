package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.medico.MedicoPageResponse;
import com.ipscentir.appointments.application.dto.medico.MedicoSearchCriteria;
import com.ipscentir.appointments.application.dto.medico.MedicoSummaryDTO;
import com.ipscentir.appointments.application.exception.MedicoNotFoundException;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Punto único de lectura de médicos ({@code hc.medicos}).
 */
@Service
@RequiredArgsConstructor
public class MedicoLookupService {

    private final SpecialistJpaRepository specialistJpaRepository;

    @Transactional(readOnly = true)
    public Specialist requireById(String medicoId) {
        return specialistJpaRepository.findById(medicoId)
                .orElseThrow(() -> new MedicoNotFoundException(medicoId));
    }

    @Transactional(readOnly = true)
    public Specialist requireActiveById(String medicoId) {
        Specialist specialist = requireById(medicoId);
        if (!specialist.isActive()) {
            throw new MedicoNotFoundException(medicoId);
        }
        return specialist;
    }

    @Transactional(readOnly = true)
    public Optional<Specialist> findById(String medicoId) {
        if (medicoId == null || medicoId.isBlank()) {
            return Optional.empty();
        }
        return specialistJpaRepository.findById(medicoId.trim());
    }

    @Transactional(readOnly = true)
    public List<Specialist> findAllActive() {
        return specialistJpaRepository.findAllByActiveTrue();
    }

    @Transactional(readOnly = true)
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
                PageRequest.of(page, size, Sort.by("lastName", "firstName"))
        );

        return new MedicoPageResponse(
                result.getContent().stream().map(this::toSummary).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public MedicoSummaryDTO getSummaryById(String medicoId) {
        return toSummary(requireById(medicoId));
    }

    @Transactional(readOnly = true)
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
                specialist.getSpecialty(),
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
}
