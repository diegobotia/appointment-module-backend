package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicoProfileResolverTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private SpecialistJpaRepository specialistJpaRepository;

    @InjectMocks
    private MedicoProfileResolver resolver;

    @Test
    void shouldResolveMedicoIdFromProfile() {
        UUID profileId = UUID.randomUUID();
        String medicoId = UUID.randomUUID().toString();
        Profile profile = Profile.builder().id(profileId).medicoId(medicoId).build();
        Specialist specialist = Specialist.builder().id(medicoId).active(true).build();

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(specialistJpaRepository.findByIdText(medicoId)).thenReturn(Optional.of(specialist));

        assertEquals(medicoId, resolver.requireMedicoIdForProfile(profileId));
    }

    @Test
    void shouldRejectProfileWithoutMedicoId() {
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder().id(profileId).build();

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        assertThrows(AccessDeniedException.class, () -> resolver.requireMedicoIdForProfile(profileId));
    }

    @Test
    void shouldRejectInactiveMedico() {
        UUID profileId = UUID.randomUUID();
        String medicoId = UUID.randomUUID().toString();
        Profile profile = Profile.builder().id(profileId).medicoId(medicoId).build();

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(specialistJpaRepository.findByIdText(medicoId))
                .thenReturn(Optional.of(Specialist.builder().id(medicoId).active(false).build()));

        assertThrows(AccessDeniedException.class, () -> resolver.requireMedicoIdForProfile(profileId));
    }
}
