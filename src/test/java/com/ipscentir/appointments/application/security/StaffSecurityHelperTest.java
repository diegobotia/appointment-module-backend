package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.security.StaffPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffSecurityHelperTest {

    @Mock
    private MedicoProfileResolver medicoProfileResolver;

    @InjectMocks
    private StaffSecurityHelper staffSecurityHelper;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireDoctorIdForMedicoUsesMedicoIdNotProfileId() {
        UUID profileId = UUID.randomUUID();
        String medicoId = UUID.randomUUID().toString();

        StaffPrincipal principal = new StaffPrincipal(
                profileId,
                Optional.of(RoleName.MEDICO),
                true,
                Optional.of(medicoId)
        );
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", profileId.toString())
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(),
                profileId.toString()
        );
        authentication.setDetails(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals(medicoId, staffSecurityHelper.requireDoctorIdForMedico());
    }

    @Test
    void requireDoctorIdForMedicoFallsBackToProfileLookup() {
        UUID profileId = UUID.randomUUID();
        String medicoId = UUID.randomUUID().toString();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        profileId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEDICO"))
                )
        );
        when(medicoProfileResolver.requireMedicoIdForProfile(profileId)).thenReturn(medicoId);

        // @WithMockUser path: no StaffPrincipal, resolver loads from DB
        assertEquals(medicoId, staffSecurityHelper.requireDoctorIdForMedico());
    }
}
