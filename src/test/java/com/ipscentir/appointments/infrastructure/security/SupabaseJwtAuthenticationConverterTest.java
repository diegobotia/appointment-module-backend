package com.ipscentir.appointments.infrastructure.security;

import com.ipscentir.appointments.application.security.MedicoProfileResolver;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.RoleRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupabaseJwtAuthenticationConverterTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MedicoProfileResolver medicoProfileResolver;

    @InjectMocks
    private SupabaseJwtAuthenticationConverter converter;

    @Test
    void shouldUseProfileRoleWhenJwtOnlyHasAuthenticatedClaim() {
        UUID profileId = UUID.fromString("5d40fca9-a0f6-4d8b-904e-b2ccef4a327f");
        UUID roleId = UUID.fromString("ce044ba3-b0e7-4114-961c-5173a00adf01");

        Profile profile = Profile.builder()
                .id(profileId)
                .roleId(roleId)
                .estaActivo(true)
                .build();
        Role role = Role.builder()
                .id(roleId)
                .nombre("Administracion")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", profileId.toString())
                .claim("role", "authenticated")
                .build();

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        JwtAuthenticationToken auth = (JwtAuthenticationToken) converter.convert(jwt);

        assertEquals(profileId.toString(), auth.getName());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_ADMINISTRACION", auth.getAuthorities().iterator().next().getAuthority());
        assertInstanceOf(StaffPrincipal.class, auth.getDetails());
        StaffPrincipal principal = (StaffPrincipal) auth.getDetails();
        assertEquals(RoleName.ADMINISTRACION, principal.roleName().orElseThrow());
    }

    @Test
    void shouldMapFacturacionRoleWithAccentFromDatabase() {
        UUID profileId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        Profile profile = Profile.builder()
                .id(profileId)
                .roleId(roleId)
                .estaActivo(true)
                .build();
        Role role = Role.builder()
                .id(roleId)
                .nombre("Facturación")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", profileId.toString())
                .claim("role", "authenticated")
                .build();

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        JwtAuthenticationToken auth = (JwtAuthenticationToken) converter.convert(jwt);

        assertEquals(new SimpleGrantedAuthority("ROLE_FACTURACION"), auth.getAuthorities().iterator().next());
    }
}
