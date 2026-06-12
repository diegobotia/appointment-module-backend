package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.RoleRepository;
import com.ipscentir.appointments.infrastructure.security.StaffPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService {

    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;

    @Value("${auth.module.base-url:http://localhost:8081}")
    private String authModuleBaseUrl;

    public Map<String, Object> getAuthConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("loginUrl", authModuleBaseUrl + "/api/v1/auth/login");
        return config;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        log.debug("Refrescando token JWT");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "El refresco de token es gestionado por el módulo de autenticación externo");
        response.put("refreshTokenReceived", refreshToken != null && !refreshToken.isBlank());
        return response;
    }

    public Map<String, Object> getCurrentUserProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> profile = new HashMap<>();

        if (auth == null || !auth.isAuthenticated()) {
            profile.put("error", "No authenticated user");
            return profile;
        }

        if (auth instanceof JwtAuthenticationToken jwtAuth
                && jwtAuth.getDetails() instanceof StaffPrincipal principal) {
            profile.put("id", principal.profileId());
            profile.put("role", principal.roleName().map(RoleName::getNombre).orElse(null));
            principal.medicoId().ifPresent(medicoId -> profile.put("medicoId", medicoId));
        }

        try {
            UUID profileId = UUID.fromString(auth.getName());
            profileRepository.findById(profileId).ifPresent(p -> {
                profile.put("email", p.getEmail());
                profile.put("name", p.getName());
                profile.put("roleId", p.getRoleId());
                if (p.getMedicoId() != null && !p.getMedicoId().isBlank()) {
                    profile.putIfAbsent("medicoId", p.getMedicoId().trim());
                }
                roleRepository.findById(p.getRoleId())
                        .ifPresent(role -> profile.put("roleNombre", role.getNombre()));
            });
        } catch (IllegalArgumentException ex) {
            profile.put("message", "Invalid subject in token: " + auth.getName());
        }

        return profile;
    }
}
