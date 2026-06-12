package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.RoleRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Role;
import com.ipscentir.appointments.infrastructure.security.StaffPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.of;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthApplicationService authApplicationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authApplicationService, "authModuleBaseUrl", "http://localhost:8081");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthConfigExposesLoginUrl() {
        Map<String, Object> config = authApplicationService.getAuthConfig();

        assertEquals("http://localhost:8081/api/v1/auth/login", config.get("loginUrl"));
    }

    @Test
    void getCurrentUserProfileFromStaffPrincipal() {
        UUID profileId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        StaffPrincipal principal = new StaffPrincipal(profileId, of(RoleName.MEDICO), true, of("medico-hc-1"));
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", profileId.toString())
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of(), profileId.toString());
        authentication.setDetails(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setEmail("medico@test.com");
        profile.setName("Dr Test");
        profile.setRoleId(roleId);
        profile.setMedicoId("medico-hc-1");
        Role role = new Role();
        role.setId(roleId);
        role.setNombre(RoleName.MEDICO.getNombre());

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        Map<String, Object> result = authApplicationService.getCurrentUserProfile();

        assertEquals(profileId, result.get("id"));
        assertEquals("Medico", result.get("role"));
        assertEquals("medico-hc-1", result.get("medicoId"));
        assertEquals("medico@test.com", result.get("email"));
    }

    @Test
    void refreshTokenAcknowledgesInput() {
        Map<String, Object> response = authApplicationService.refreshToken("refresh-abc");
        assertTrue((Boolean) response.get("refreshTokenReceived"));
    }
}
