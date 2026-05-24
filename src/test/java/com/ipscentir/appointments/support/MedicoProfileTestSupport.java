package com.ipscentir.appointments.support;

import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.RoleJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile;

import java.util.UUID;

/**
 * Crea un médico en hc.medicos y un perfil de login en core.profiles con IDs distintos.
 */
public final class MedicoProfileTestSupport {

    private MedicoProfileTestSupport() {
    }

    public record MedicoTestIdentity(String medicoId, UUID profileId) {
    }

    public static MedicoTestIdentity seedMedicoWithProfile(
            SpecialistJpaRepository specialistJpaRepository,
            ProfileRepository profileRepository,
            RoleJpaRepository roleJpaRepository,
            String numeroMedico,
            String firstName,
            String lastName
    ) {
        String medicoId = UUID.randomUUID().toString();
        UUID profileId = UUID.randomUUID();

        var medicoRole = roleJpaRepository.findByName(RoleName.MEDICO)
                .orElseGet(() -> roleJpaRepository.save(
                        com.ipscentir.appointments.domain.model.security.Role.builder()
                                .name(RoleName.MEDICO)
                                .build()
                ));

        specialistJpaRepository.save(Specialist.builder()
                .id(medicoId)
                .numeroMedico(numeroMedico)
                .firstName(firstName)
                .lastName(lastName)
                .specialty("Medicina general")
                .active(true)
                .build());

        profileRepository.save(Profile.builder()
                .id(profileId)
                .roleId(medicoRole.getId())
                .medicoId(medicoId)
                .name(firstName + " " + lastName)
                .email("medico-" + profileId + "@test.local")
                .estaActivo(true)
                .build());

        return new MedicoTestIdentity(medicoId, profileId);
    }
}
