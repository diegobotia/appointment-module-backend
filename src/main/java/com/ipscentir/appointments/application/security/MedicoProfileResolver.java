package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MedicoProfileResolver {

    private final ProfileRepository profileRepository;
    private final SpecialistJpaRepository specialistJpaRepository;

    public String requireMedicoIdForProfile(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new AccessDeniedException("Perfil no encontrado"));
        return requireMedicoId(profile);
    }

    public String requireMedicoId(Profile profile) {
        String medicoId = normalizeMedicoId(profile.getMedicoId());
        if (medicoId == null) {
            throw new AccessDeniedException("El perfil no tiene médico vinculado (medico_id)");
        }

        Specialist specialist = specialistJpaRepository.findByIdText(medicoId)
                .orElseThrow(() -> new AccessDeniedException("Médico no encontrado en hc.medicos: " + medicoId));
        if (!specialist.isActive()) {
            throw new AccessDeniedException("El médico vinculado no está activo");
        }
        return medicoId;
    }

    public Optional<String> findMedicoIdForProfile(UUID profileId) {
        return profileRepository.findById(profileId)
                .map(Profile::getMedicoId)
                .map(this::normalizeMedicoId);
    }

    private String normalizeMedicoId(String medicoId) {
        if (medicoId == null) {
            return null;
        }
        String trimmed = medicoId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
