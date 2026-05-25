package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.security.StaffPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StaffSecurityHelper {

    private final MedicoProfileResolver medicoProfileResolver;

    public StaffPrincipal requireStaffPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth
                && jwtAuth.getDetails() instanceof StaffPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Se requiere autenticación de personal interno");
    }

    public Optional<StaffPrincipal> currentStaffPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth
                && jwtAuth.getDetails() instanceof StaffPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public boolean hasRole(RoleName roleName) {
        return currentStaffPrincipal()
                .flatMap(StaffPrincipal::roleName)
                .map(role -> role == roleName)
                .orElseGet(() -> hasAuthority(roleName.getAuthority()));
    }

    public boolean hasAnyRole(RoleName... roles) {
        Optional<RoleName> current = currentStaffPrincipal().flatMap(StaffPrincipal::roleName);
        if (current.isPresent()) {
            for (RoleName role : roles) {
                if (current.get() == role) {
                    return true;
                }
            }
            return false;
        }
        for (RoleName role : roles) {
            if (hasAuthority(role.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public UUID requireProfileId() {
        return currentStaffPrincipal()
                .map(StaffPrincipal::profileId)
                .orElseGet(() -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication == null) {
                        throw new AccessDeniedException("Se requiere autenticación de personal interno");
                    }
                    try {
                        return UUID.fromString(authentication.getName());
                    } catch (IllegalArgumentException ex) {
                        throw new AccessDeniedException("Identificador de perfil inválido en el token");
                    }
                });
    }

    public String requireDoctorIdForMedico() {
        if (!hasRole(RoleName.MEDICO)) {
            throw new AccessDeniedException("Solo médicos usan el identificador de agenda propia");
        }
        return currentStaffPrincipal()
                .flatMap(StaffPrincipal::medicoId)
                .orElseGet(() -> medicoProfileResolver.requireMedicoIdForProfile(requireProfileId()));
    }

    public boolean isOwnMedicoIdOrProfileId(String requestedId) {
        if (requestedId == null || requestedId.isBlank() || !hasRole(RoleName.MEDICO)) {
            return false;
        }

        String trimmedRequestedId = requestedId.trim();
        StaffPrincipal principal = currentStaffPrincipal().orElse(null);
        if (principal == null) {
            return false;
        }

        if (principal.medicoId().map(trimmedRequestedId::equals).orElse(false)) {
            return true;
        }

        return principal.profileId().toString().equals(trimmedRequestedId);
    }

    public String resolveOwnMedicoIdOrRequestedId(String requestedId) {
        if (requestedId == null || requestedId.isBlank() || !hasRole(RoleName.MEDICO)) {
            return requestedId;
        }

        String trimmedRequestedId = requestedId.trim();
        StaffPrincipal principal = currentStaffPrincipal().orElse(null);
        if (principal == null) {
            return trimmedRequestedId;
        }

        if (principal.medicoId().map(trimmedRequestedId::equals).orElse(false)) {
            return trimmedRequestedId;
        }

        if (principal.profileId().toString().equals(trimmedRequestedId)) {
            return medicoProfileResolver.requireMedicoIdForProfile(principal.profileId());
        }

        return trimmedRequestedId;
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }
}
