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

    @Value("${supabase.url:https://project.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.api-key:public-anon-key}")
    private String supabaseAnonKey;

    public Map<String, Object> getSupabaseAuthConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("supabaseUrl", supabaseUrl);
        config.put("supabaseAnonKey", supabaseAnonKey);
        config.put("jwkSetUri", supabaseUrl + "/auth/v1/keys");
        config.put("issuerUri", supabaseUrl + "/auth/v1");
        return config;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        log.debug("Refrescando token JWT");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Implementar integración con Supabase Auth");
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
            profile.put("role", principal.roleName().map(RoleName::getSupabaseNombre).orElse(null));
        }

        try {
            UUID profileId = UUID.fromString(auth.getName());
            profileRepository.findById(profileId).ifPresent(p -> {
                profile.put("email", p.getEmail());
                profile.put("name", p.getName());
                profile.put("roleId", p.getRoleId());
                roleRepository.findById(p.getRoleId())
                        .ifPresent(role -> profile.put("roleNombre", role.getNombre()));
            });
        } catch (IllegalArgumentException ex) {
            profile.put("message", "Invalid subject in token: " + auth.getName());
        }

        return profile;
    }
}
